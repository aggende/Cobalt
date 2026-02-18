package com.github.auties00.cobalt.test;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.client.WhatsAppWebClientHistory;

public class CobaltLibraryTest {

    private static final System.Logger LOGGER = System.getLogger(CobaltLibraryTest.class.getName());

    public static void main(String[] args) {
        var options = WhatsAppClient.builder()
                .webClient()
                .loadLastOrCreateConnection()
                .historySetting(WhatsAppWebClientHistory.extended(true));

        var registeredClient = options.registered();
        WhatsAppClient client;
        if (registeredClient.isPresent()) {
            LOGGER.log(System.Logger.Level.INFO, "Session already paired, connecting without QR code...");
            client = registeredClient.get();
        } else {
            LOGGER.log(System.Logger.Level.INFO, "No existing session found, generating QR code...");
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

    private static void log(String message) {
        LOGGER.log(System.Logger.Level.INFO, message);
    }

    private static void registerAllListeners(WhatsAppClient client) {
        client.addLoggedInListener(api ->
                log("[LOGGED_IN] Connected successfully. Privacy settings: " + api.store().privacySettings())
        );

        client.addDisconnectedListener((api, reason) ->
                log("[DISCONNECTED] Reason: " + reason)
        );

        client.addNodeSentListener((api, outgoing) ->
                LOGGER.log(System.Logger.Level.DEBUG, "[NODE_SENT] " + outgoing)
        );

        client.addNodeReceivedListener((api, incoming) ->
                LOGGER.log(System.Logger.Level.DEBUG, "[NODE_RECEIVED] " + incoming)
        );

        client.addContactsListener((api, contacts) ->
                log("[CONTACTS] Received " + contacts.size() + " contacts")
        );

        client.addContactPresenceListener((api, conversation, participant) ->
                log("[CONTACT_PRESENCE] Conversation: " + conversation + ", Participant: " + participant)
        );

        client.addChatsListener((api, chats) ->
                log("[CHATS] Received " + chats.size() + " chats")
        );

        client.addNewslettersListener((api, newsletters) ->
                log("[NEWSLETTERS] Received " + newsletters.size() + " newsletters")
        );

        client.addWebHistorySyncMessagesListener((api, chat, last) ->
                log("[HISTORY_SYNC_MESSAGES] Chat: " + chat.name()
                        + ", Messages: " + chat.messages().size()
                        + ", Last: " + last)
        );

        client.addWebHistorySyncPastParticipantsListener((api, chatJid, pastParticipants) ->
                log("[HISTORY_SYNC_PAST_PARTICIPANTS] Chat: " + chatJid
                        + ", Participants: " + pastParticipants.size())
        );

        client.addWebHistorySyncProgressListener((api, percentage, recent) ->
                log("[HISTORY_SYNC_PROGRESS] " + percentage + "% (recent: " + recent + ")")
        );

        client.addNewMessageListener((api, info) ->
                log("[NEW_MESSAGE] " + info)
        );

        client.addMessageDeletedListener((api, info, everyone) ->
                log("[MESSAGE_DELETED] Message: " + info.id() + ", Everyone: " + everyone)
        );

        client.addMessageStatusListener((api, info) ->
                log("[MESSAGE_STATUS] Message: " + info.id() + ", Status: " + info.status())
        );

        client.addStatusListener((api, status) ->
                log("[STATUS] Received " + status.size() + " status updates")
        );

        client.addNewStatusListener((api, status) ->
                log("[NEW_STATUS] " + status)
        );

        client.addMessageReplyListener((api, response, quoted) ->
                log("[MESSAGE_REPLY] Response: " + response.id() + ", Quoted: " + quoted.id())
        );

        client.addProfilePictureChangedListener((api, jid) ->
                log("[PROFILE_PICTURE_CHANGED] Jid: " + jid)
        );

        client.addNameChangedListener((api, oldName, newName) ->
                log("[NAME_CHANGED] Old: " + oldName + " -> New: " + newName)
        );

        client.addAboutChangedListener((api, oldAbout, newAbout) ->
                log("[ABOUT_CHANGED] Old: " + oldAbout + " -> New: " + newAbout)
        );

        client.addLocaleChangedListener((api, oldLocale, newLocale) ->
                log("[LOCALE_CHANGED] Old: " + oldLocale + " -> New: " + newLocale)
        );

        client.addContactBlockedListener((api, contact) ->
                log("[CONTACT_BLOCKED] Contact: " + contact)
        );

        client.addNewContactListener((api, contact) ->
                log("[NEW_CONTACT] " + contact)
        );

        client.addPrivacySettingChangedListener((api, newPrivacyEntry) ->
                log("[PRIVACY_SETTING_CHANGED] " + newPrivacyEntry)
        );

        client.addRegistrationCodeListener((api, code) ->
                log("[REGISTRATION_CODE] Code: " + code)
        );

        client.addCallListener((api, call) ->
                log("[CALL] " + call)
        );

        client.addWebAppStateActionListener((api, action, messageIndexInfo) ->
                log("[WEB_APP_STATE_ACTION] Action: " + action + ", Info: " + messageIndexInfo)
        );

        client.addWebAppStateSettingListener((api, setting) ->
                log("[WEB_APP_STATE_SETTING] " + setting)
        );

        client.addWebAppPrimaryFeaturesListener((api, features) ->
                log("[WEB_APP_PRIMARY_FEATURES] " + features)
        );
    }
}
