package vitbuk.com.Ambotorix.harness;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatPhoto;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.stickers.*;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
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

    public record OutboundMessage(Kind kind, long chatId, String text, boolean hasPhoto, List<Button> buttons) {}

    private final Map<Long, List<OutboundMessage>> history = new HashMap<>();
    private final Set<Long> reachableChats = new HashSet<>();
    private final AtomicInteger updateSeq = new AtomicInteger(1);
    private Consumer<Update> bot;

    // ---- wiring ----
    public void bindBot(Consumer<Update> bot) { this.bot = bot; }

    public void reset() {
        history.clear();
        reachableChats.clear();
    }

    /** Append-only record of everything the bot sent to {@code chatId}, oldest first. */
    public List<OutboundMessage> history(long chatId) {
        return history.computeIfAbsent(chatId, k -> new ArrayList<>());
    }

    // ---- inbound: synthesize + deliver ----

    public void deliverMessage(String userName, long userId, long chatId, String text) {
        reachableChats.add(chatId); // inbound activity makes the chat replyable
        User from = User.builder().id(userId).firstName(userName).isBot(false).userName(userName).build();
        Chat chat = Chat.builder().id(chatId).type(chatId == userId ? "private" : "group").build();
        Message message = Message.builder()
                .messageId(updateSeq.getAndIncrement())
                .from(from).chat(chat).date((int) (System.currentTimeMillis() / 1000)).text(text)
                .build();
        Update update = new Update();
        update.setUpdateId(updateSeq.getAndIncrement());
        update.setMessage(message);
        deliver(update);
    }

    public void deliverTap(String userName, long userId, long shownInChatId, String callbackData) {
        reachableChats.add(shownInChatId);
        User from = User.builder().id(userId).firstName(userName).isBot(false).userName(userName).build();
        Chat chat = Chat.builder().id(shownInChatId).type(shownInChatId == userId ? "private" : "group").build();
        Message shown = Message.builder().messageId(updateSeq.getAndIncrement()).chat(chat).date(0).build();
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

    private void recordOrFail(Kind kind, long chatId, String text, boolean hasPhoto, ReplyKeyboard markup)
            throws TelegramApiException {
        if (!reachableChats.contains(chatId)) {
            throw new TelegramApiException("Forbidden: bot can't initiate conversation with chat " + chatId
                    + " (no prior inbound activity)");
        }
        history(chatId).add(new OutboundMessage(kind, chatId, text, hasPhoto, buttonsOf(markup)));
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
    public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) throws TelegramApiException {
        if (method instanceof SendMessage sm) {
            recordOrFail(Kind.TEXT, Long.parseLong(sm.getChatId()), sm.getText(), false, sm.getReplyMarkup());
        }
        // AnswerCallbackQuery and anything else: acknowledged, not a chat send.
        return null;
    }

    @Override
    public Message execute(SendPhoto sendPhoto) throws TelegramApiException {
        recordOrFail(Kind.PHOTO, Long.parseLong(sendPhoto.getChatId()), sendPhoto.getCaption(), true,
                sendPhoto.getReplyMarkup());
        return null;
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
