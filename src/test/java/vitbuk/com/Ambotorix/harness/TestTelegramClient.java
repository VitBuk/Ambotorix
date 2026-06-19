package vitbuk.com.Ambotorix.harness;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatPhoto;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.stickers.*;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * The test's entire production contact surface (see SCENARIO_TESTING_PLAN.md §4).
 *
 * <p><b>Output seam:</b> implements {@link TelegramClient}; the two {@code execute} paths the bot
 * actually uses ({@code SendMessage}/{@code AnswerCallbackQuery} via the generic overload, and
 * {@code SendPhoto}) are appended to a per-chat {@link #history}. Every other method is unsupported.
 *
 * <p><b>Input seam:</b> {@link #deliverMessage} / {@link #deliverTap} synthesize {@link Update}s
 * with enough fidelity for the bot's guards and routing to read real values, and feed them to the
 * bot bound via {@link #bindBot}.
 *
 * <p><b>DM reachability.</b> Telegram lets a bot message a chat only once that chat has interacted
 * with it: a group the bot is in (here, any chat that has produced an inbound update) and a private
 * chat the user has opened. We model this — a send to a chat with no prior inbound activity throws
 * {@link TelegramApiException}, exactly as production does — so scenarios reflect that players who
 * only type in the group never receive DMs.
 *
 * <p>History is a single append-only list per chat (random access). Expectations "pseudo-consume"
 * it via a cursor held by the runner; {@code <press_button>} scans the full list. Nothing is removed.
 */
public class TestTelegramClient implements TelegramClient {

    public enum Kind { TEXT, PHOTO }

    public record Button(String label, String callbackData) {}

    /**
     * One captured outbound message. {@code messageId} is the synthetic id the bot sees back (so it can
     * later edit or be replied to); {@code replyToMessageId} is the message this one backlinks to (null
     * if none); {@code edited} is true once an {@code EditMessageText} has updated it in place.
     */
    public record OutboundMessage(Kind kind, long chatId, Integer threadId, int messageId,
                                  Integer replyToMessageId, String text, boolean hasPhoto,
                                  boolean edited, List<Button> buttons) {}

    /** A logical output channel: a chat plus the forum topic within it (null = General topic / DM). */
    private record ChannelKey(long chatId, Integer threadId) {}

    private final Map<ChannelKey, List<OutboundMessage>> history = new HashMap<>();
    /** The single live "status" message per channel — posted silently, then edited in place (never in the stream). */
    private final Map<ChannelKey, OutboundMessage> statusSlot = new HashMap<>();
    /** How many times a brand-new status message was created per channel — must stay 1 (edited, not re-posted). */
    private final Map<ChannelKey, Integer> statusCreates = new HashMap<>();
    private final Set<Long> reachableChats = new HashSet<>();
    private final AtomicInteger updateSeq = new AtomicInteger(1);
    private final AtomicInteger outboundSeq = new AtomicInteger(1);
    private Consumer<Update> bot;

    // ---- wiring ----
    public void bindBot(Consumer<Update> bot) { this.bot = bot; }

    public void reset() {
        history.clear();
        statusSlot.clear();
        statusCreates.clear();
        reachableChats.clear();
    }

    /** Append-only record of everything the bot sent to {@code chatId} in topic {@code threadId}, oldest first. */
    public List<OutboundMessage> history(long chatId, Integer threadId) {
        return history.computeIfAbsent(new ChannelKey(chatId, threadId), k -> new ArrayList<>());
    }

    /** The current (latest-edited) live status message of a channel, or null if none was posted. */
    public OutboundMessage statusMessage(long chatId, Integer threadId) {
        return statusSlot.get(new ChannelKey(chatId, threadId));
    }

    /** How many distinct status messages were created for a channel — 1 means "edited in place, not re-posted". */
    public int statusCreateCount(long chatId, Integer threadId) {
        return statusCreates.getOrDefault(new ChannelKey(chatId, threadId), 0);
    }

    /** Message id of a channel's live status message (the target a milestone reply backlinks to), or null. */
    public Integer statusMessageId(long chatId, Integer threadId) {
        OutboundMessage m = statusSlot.get(new ChannelKey(chatId, threadId));
        return m == null ? null : m.messageId();
    }

    // ---- inbound: synthesize + deliver ----

    public void deliverMessage(String userName, long userId, long chatId, Integer threadId, String text) {
        reachableChats.add(chatId); // inbound activity makes the chat replyable
        User from = User.builder().id(userId).firstName(userName).isBot(false).userName(userName).build();
        Chat chat = Chat.builder().id(chatId).type(chatId == userId ? "private" : "group").build();
        Message message = Message.builder()
                .messageId(updateSeq.getAndIncrement())
                .from(from).chat(chat).messageThreadId(threadId)
                .date((int) (System.currentTimeMillis() / 1000)).text(text)
                .build();
        Update update = new Update();
        update.setUpdateId(updateSeq.getAndIncrement());
        update.setMessage(message);
        deliver(update);
    }

    public void deliverTap(String userName, long userId, long shownInChatId, Integer threadId, String callbackData) {
        reachableChats.add(shownInChatId);
        User from = User.builder().id(userId).firstName(userName).isBot(false).userName(userName).build();
        Chat chat = Chat.builder().id(shownInChatId).type(shownInChatId == userId ? "private" : "group").build();
        Message shown = Message.builder().messageId(updateSeq.getAndIncrement()).chat(chat)
                .messageThreadId(threadId).date(0).build();
        CallbackQuery cq = new CallbackQuery();
        cq.setId("cb" + updateSeq.getAndIncrement());
        cq.setFrom(from);
        cq.setMessage(shown);
        cq.setChatInstance("test");
        cq.setData(callbackData);
        Update update = new Update();
        update.setUpdateId(updateSeq.getAndIncrement());
        update.setCallbackQuery(cq);
        deliver(update);
    }

    private void deliver(Update update) {
        if (bot == null) throw new IllegalStateException("bot not bound — call bindBot() first");
        bot.accept(update);
    }

    // ---- outbound capture ----

    private void requireReachable(long chatId) throws TelegramApiException {
        if (!reachableChats.contains(chatId)) {
            throw new TelegramApiException("Forbidden: bot can't initiate conversation with chat " + chatId
                    + " (no prior inbound activity)");
        }
    }

    /** A silent message (disable_notification) is the live status message: it goes to the status slot, not the stream. */
    private void putStatus(long chatId, Integer threadId, OutboundMessage msg) throws TelegramApiException {
        requireReachable(chatId);
        ChannelKey key = new ChannelKey(chatId, threadId);
        statusSlot.put(key, msg);
        statusCreates.merge(key, 1, Integer::sum);
    }

    private void recordStream(long chatId, Integer threadId, OutboundMessage msg) throws TelegramApiException {
        requireReachable(chatId);
        history(chatId, threadId).add(msg);
    }

    /** Apply an in-place edit to the status message identified by (chatId, messageId). */
    private void applyEdit(long chatId, int messageId, String text) throws TelegramApiException {
        for (Map.Entry<ChannelKey, OutboundMessage> e : statusSlot.entrySet()) {
            OutboundMessage cur = e.getValue();
            if (e.getKey().chatId() == chatId && cur.messageId() == messageId) {
                e.setValue(new OutboundMessage(cur.kind(), cur.chatId(), cur.threadId(), cur.messageId(),
                        cur.replyToMessageId(), text, cur.hasPhoto(), true, cur.buttons()));
                return;
            }
        }
        throw new TelegramApiException("EditMessageText: no live message " + messageId + " in chat " + chatId);
    }

    private Message stub(long chatId, int messageId) {
        Chat chat = Chat.builder().id(chatId).type(chatId > 0 && chatId < 1000 ? "private" : "group").build();
        return Message.builder().messageId(messageId).chat(chat).date(0).build();
    }

    private List<Button> buttonsOf(ReplyKeyboard markup) {
        List<Button> out = new ArrayList<>();
        if (markup instanceof InlineKeyboardMarkup ikm && ikm.getKeyboard() != null) {
            for (InlineKeyboardRow row : ikm.getKeyboard()) {
                for (InlineKeyboardButton b : row) {
                    out.add(new Button(b.getText(), b.getCallbackData()));
                }
            }
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) throws TelegramApiException {
        if (method instanceof SendMessage sm) {
            long chatId = Long.parseLong(sm.getChatId());
            int id = outboundSeq.getAndIncrement();
            OutboundMessage msg = new OutboundMessage(Kind.TEXT, chatId, sm.getMessageThreadId(), id,
                    sm.getReplyToMessageId(), sm.getText(), false, false, buttonsOf(sm.getReplyMarkup()));
            if (Boolean.TRUE.equals(sm.getDisableNotification())) {
                putStatus(chatId, sm.getMessageThreadId(), msg);
            } else {
                recordStream(chatId, sm.getMessageThreadId(), msg);
            }
            return (T) stub(chatId, id);
        }
        if (method instanceof EditMessageText et) {
            applyEdit(Long.parseLong(et.getChatId()), et.getMessageId(), et.getText());
            return (T) Boolean.TRUE;
        }
        // AnswerCallbackQuery and anything else: acknowledged, not a chat send.
        return null;
    }

    @Override
    public Message execute(SendPhoto sendPhoto) throws TelegramApiException {
        long chatId = Long.parseLong(sendPhoto.getChatId());
        int id = outboundSeq.getAndIncrement();
        OutboundMessage msg = new OutboundMessage(Kind.PHOTO, chatId, sendPhoto.getMessageThreadId(), id, null,
                sendPhoto.getCaption(), true, false, buttonsOf(sendPhoto.getReplyMarkup()));
        recordStream(chatId, sendPhoto.getMessageThreadId(), msg);
        return stub(chatId, id);
    }

    // ---- everything else: not used by the bot ----

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("TestTelegramClient does not support this method");
    }

    @Override public <T extends Serializable, Method extends BotApiMethod<T>> CompletableFuture<T> executeAsync(Method method) { throw unsupported(); }
    @Override public Message execute(SendDocument sendDocument) { throw unsupported(); }
    @Override public Boolean execute(SetWebhook setWebhook) { throw unsupported(); }
    @Override public Message execute(SendVideo sendVideo) { throw unsupported(); }
    @Override public Message execute(SendVideoNote sendVideoNote) { throw unsupported(); }
    @Override public Message execute(SendSticker sendSticker) { throw unsupported(); }
    @Override public Message execute(SendAudio sendAudio) { throw unsupported(); }
    @Override public Message execute(SendVoice sendVoice) { throw unsupported(); }
    @Override public List<Message> execute(SendMediaGroup sendMediaGroup) { throw unsupported(); }
    @Override public List<Message> execute(SendPaidMedia sendPaidMedia) { throw unsupported(); }
    @Override public Boolean execute(SetChatPhoto setChatPhoto) { throw unsupported(); }
    @Override public Boolean execute(AddStickerToSet addStickerToSet) { throw unsupported(); }
    @Override public Boolean execute(ReplaceStickerInSet replaceStickerInSet) { throw unsupported(); }
    @Override public Boolean execute(SetStickerSetThumbnail setStickerSetThumbnail) { throw unsupported(); }
    @Override public Boolean execute(CreateNewStickerSet createNewStickerSet) { throw unsupported(); }
    @Override public File execute(UploadStickerFile uploadStickerFile) { throw unsupported(); }
    @Override public Serializable execute(EditMessageMedia editMessageMedia) { throw unsupported(); }
    @Override public Message execute(SendAnimation sendAnimation) { throw unsupported(); }
    @Override public java.io.File downloadFile(File file) { throw unsupported(); }
    @Override public InputStream downloadFileAsStream(File file) { throw unsupported(); }
    @Override public CompletableFuture<Message> executeAsync(SendDocument sendDocument) { throw unsupported(); }
    @Override public CompletableFuture<Message> executeAsync(SendPhoto sendPhoto) { throw unsupported(); }
    @Override public CompletableFuture<Boolean> executeAsync(SetWebhook setWebhook) { throw unsupported(); }
    @Override public CompletableFuture<Message> executeAsync(SendVideo sendVideo) { throw unsupported(); }
    @Override public CompletableFuture<Message> executeAsync(SendVideoNote sendVideoNote) { throw unsupported(); }
    @Override public CompletableFuture<Message> executeAsync(SendSticker sendSticker) { throw unsupported(); }
    @Override public CompletableFuture<Message> executeAsync(SendAudio sendAudio) { throw unsupported(); }
    @Override public CompletableFuture<Message> executeAsync(SendVoice sendVoice) { throw unsupported(); }
    @Override public CompletableFuture<List<Message>> executeAsync(SendMediaGroup sendMediaGroup) { throw unsupported(); }
    @Override public CompletableFuture<List<Message>> executeAsync(SendPaidMedia sendPaidMedia) { throw unsupported(); }
    @Override public CompletableFuture<Boolean> executeAsync(SetChatPhoto setChatPhoto) { throw unsupported(); }
    @Override public CompletableFuture<Boolean> executeAsync(AddStickerToSet addStickerToSet) { throw unsupported(); }
    @Override public CompletableFuture<Boolean> executeAsync(ReplaceStickerInSet replaceStickerInSet) { throw unsupported(); }
    @Override public CompletableFuture<Boolean> executeAsync(SetStickerSetThumbnail setStickerSetThumbnail) { throw unsupported(); }
    @Override public CompletableFuture<Boolean> executeAsync(CreateNewStickerSet createNewStickerSet) { throw unsupported(); }
    @Override public CompletableFuture<File> executeAsync(UploadStickerFile uploadStickerFile) { throw unsupported(); }
    @Override public CompletableFuture<Serializable> executeAsync(EditMessageMedia editMessageMedia) { throw unsupported(); }
    @Override public CompletableFuture<Message> executeAsync(SendAnimation sendAnimation) { throw unsupported(); }
    @Override public CompletableFuture<java.io.File> downloadFileAsync(File file) { throw unsupported(); }
    @Override public CompletableFuture<InputStream> downloadFileAsStreamAsync(File file) { throw unsupported(); }
}
