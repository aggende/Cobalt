package com.github.auties00.cobalt.test;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.client.WhatsAppWebClientHistory;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.cobalt.model.info.NewsletterMessageInfo;
import com.github.auties00.cobalt.model.message.standard.TextMessage;
import com.github.auties00.cobalt.store.WhatsappStoreSerializer;
import com.alibaba.fastjson2.JSON;

import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class CobaltLibraryTest {

    private static final Path SESSION_PATH = Path.of(".cobalt-sessions");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String GRAY = "\u001B[90m";
    private static final String BOLD = "\u001B[1m";

    public static void main(String[] args) {
        var serializer = WhatsappStoreSerializer.toProtobuf(SESSION_PATH);
        var options = WhatsAppClient.builder()
                .webClient(serializer)
                .loadLastOrCreateConnection()
                .historySetting(WhatsAppWebClientHistory.extended(true))
                .name("Cobalt Test Client");

        var registeredClient = options.registered();
        WhatsAppClient client;
        if (registeredClient.isPresent()) {
            info("SESSION", "Session already paired, connecting without QR code...");
            client = registeredClient.get();
        } else {
            warn("SESSION", "No existing session found, generating QR code...");
            client = options.unregistered(
                    WhatsAppClientVerificationHandler.Web.QrCode.toFile(
                            WhatsAppClientVerificationHandler.Web.QrCode.ToFile.toDesktop()
                    )
            );
        }

        registerAllListeners(client);

        client.connect()
                .waitForDisconnection();
    }

    private static void print(String color, String level, String tag, String message) {
        var time = LocalTime.now().format(TIME_FMT);
        System.out.printf("%s%s%s %s%-5s%s %s[%-30s]%s %s%n",
                GRAY, time, RESET,
                color, level, RESET,
                BOLD, tag, RESET,
                message);
    }

    private static void info(String tag, String message) {
        print(GREEN, "INFO", tag, message);
    }

    private static void warn(String tag, String message) {
        print(YELLOW, "WARN", tag, message);
    }

    private static void event(String tag, String message) {
        print(CYAN, "EVENT", tag, message);
    }

    private static void data(String tag, String message) {
        print(BLUE, "DATA", tag, message);
    }

    private static void debug(String tag, String message) {
        print(GRAY, "DEBUG", tag, message);
    }

    private static String json(Object obj) {
        return JSON.toJSONString(obj);
    }

    private static String formatMessageInfo(MessageInfo info) {
        var sb = new StringBuilder();
        sb.append("id=").append(info.id());
        sb.append(" | from=").append(info.senderJid());
        sb.append(" | type=").append(info.message().type());
        if (info instanceof ChatMessageInfo chatInfo) {
            sb.append(" | chat=").append(chatInfo.chatJid());
            chatInfo.pushName().ifPresent(name -> sb.append(" | pushName=").append(name));
        } else if (info instanceof NewsletterMessageInfo newsletterInfo) {
            sb.append(" | newsletter=").append(newsletterInfo.newsletter().jid());
        }
        var content = info.message().content();
        if (content instanceof TextMessage textMsg) {
            sb.append(" | text=").append(textMsg.text());
        }
        return sb.toString();
    }

    private static void registerAllListeners(WhatsAppClient client) {
        client.addLoggedInListener(api ->
                info("LOGGED_IN", "Connected successfully. Privacy: " + json(api.store().privacySettings()))
        );

        client.addDisconnectedListener((api, reason) ->
                warn("DISCONNECTED", "Reason: " + reason)
        );

        client.addNodeSentListener((api, outgoing) ->
                debug("NODE_SENT", json(outgoing))
        );

        client.addNodeReceivedListener((api, incoming) ->
                debug("NODE_RECEIVED", json(incoming))
        );

        client.addContactsListener((api, contacts) ->
                data("CONTACTS", "Received " + contacts.size() + " contacts")
        );

        client.addContactPresenceListener((api, conversation, participant) ->
                event("CONTACT_PRESENCE", "Conversation: " + conversation + " | Participant: " + participant)
        );

        client.addChatsListener((api, chats) ->
                data("CHATS", "Received " + chats.size() + " chats")
        );

        client.addNewslettersListener((api, newsletters) ->
                data("NEWSLETTERS", "Received " + newsletters.size() + " newsletters")
        );

        client.addWebHistorySyncMessagesListener((api, chat, last) ->
                data("HISTORY_MESSAGES", "Chat: " + chat.name()
                        + " | Messages: " + chat.messages().size()
                        + " | Done: " + last)
        );

        client.addWebHistorySyncPastParticipantsListener((api, chatJid, pastParticipants) ->
                data("HISTORY_PARTICIPANTS", "Chat: " + chatJid + " | Participants: " + json(pastParticipants))
        );

        client.addWebHistorySyncProgressListener((api, percentage, recent) -> {
                data("HISTORY_PROGRESS", percentage + "% (recent: " + recent + ")");
                if (percentage == 100) {
                    var mappingCount = api.store().lidMappingCount();
                    info("LID_MAPPINGS", "Total LID mappings registered: " + mappingCount);
                }
        });

        client.addNewMessageListener((api, info) ->
                event("NEW_MESSAGE", formatMessageInfo(info))
        );

        client.addMessageDeletedListener((api, info, everyone) ->
                event("MSG_DELETED", json(info) + " | Everyone: " + everyone)
        );

        client.addMessageStatusListener((api, info) ->
                event("MSG_STATUS", json(info))
        );

        client.addStatusListener((api, status) ->
                data("STATUS_LIST", "Received " + status.size() + " status updates")
        );

        client.addNewStatusListener((api, status) ->
                event("NEW_STATUS", json(status))
        );

        client.addMessageReplyListener((api, response, quoted) ->
                event("MSG_REPLY", "Response: " + json(response) + " | Quoted: " + json(quoted))
        );

        client.addProfilePictureChangedListener((api, jid) ->
                event("PROFILE_PIC", "Jid: " + jid)
        );

        client.addNameChangedListener((api, oldName, newName) ->
                event("NAME_CHANGED", oldName + " -> " + newName)
        );

        client.addAboutChangedListener((api, oldAbout, newAbout) ->
                event("ABOUT_CHANGED", oldAbout + " -> " + newAbout)
        );

        client.addLocaleChangedListener((api, oldLocale, newLocale) ->
                event("LOCALE_CHANGED", oldLocale + " -> " + newLocale)
        );

        client.addContactBlockedListener((api, contact) ->
                warn("CONTACT_BLOCKED", "Contact: " + contact)
        );

        client.addNewContactListener((api, contact) ->
                event("NEW_CONTACT", json(contact))
        );

        client.addPrivacySettingChangedListener((api, newPrivacyEntry) ->
                event("PRIVACY_CHANGED", json(newPrivacyEntry))
        );

        client.addRegistrationCodeListener((api, code) ->
                info("REG_CODE", "Code: " + code)
        );

        client.addCallListener((api, call) ->
                event("CALL", json(call))
        );

        client.addWebAppStateActionListener((api, action, messageIndexInfo) ->
                event("STATE_ACTION", "Action: " + json(action) + " | Info: " + json(messageIndexInfo))
        );

        client.addWebAppStateSettingListener((api, setting) ->
                event("STATE_SETTING", json(setting))
        );

        client.addWebAppPrimaryFeaturesListener((api, features) ->
                data("FEATURES", json(features))
        );
    }
}
