package com.github.auties00.cobalt.socket.notification;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.model.contact.ContactStatus;
import com.github.auties00.cobalt.socket.SocketStream;

import java.time.ZonedDateTime;

public final class PresenceStreamNodeHandler extends SocketStream.Handler {
    public PresenceStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "presence", "chatstate");
    }

    @Override
    public void handle(Node node) {
        var status = getUpdateType(node);
        var chatJid = node.getRequiredAttributeAsJid("from");
        var resolvedChatJid = resolveLidToPhone(chatJid);
        var participantJid = node.getAttributeAsJid("participant");
        if(participantJid.isEmpty()) {
            whatsapp.store()
                    .findContactByJid(resolvedChatJid)
                    .ifPresent(contact -> {
                        contact.setLastKnownPresence(status);
                        contact.setLastSeen(ZonedDateTime.now());
                    });
            for(var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onContactPresence(whatsapp, resolvedChatJid, resolvedChatJid));
            }
        } else {
            var resolvedParticipantJid = resolveLidToPhone(participantJid.get());
            whatsapp.store()
                    .findContactByJid(resolvedChatJid)
                    .ifPresent(contact -> {
                        contact.setLastKnownPresence(ContactStatus.AVAILABLE);
                        contact.setLastSeen(ZonedDateTime.now());
                    });
            whatsapp.store()
                    .findChatByJid(resolvedChatJid)
                    .ifPresent(chat -> chat.addPresence(resolvedParticipantJid, status));
            for(var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onContactPresence(whatsapp, resolvedChatJid, resolvedParticipantJid));
            }
        }
    }

    private Jid resolveLidToPhone(Jid jid) {
        if (!jid.hasLidServer()) {
            return jid;
        }
        return whatsapp.store().findPhoneByLid(jid).orElse(jid);
    }
    private ContactStatus getUpdateType(Node node) {
        var media = node.getChild()
                .flatMap(entry -> entry.getAttributeAsString("media"))
                .orElse("");
        if (media.equals("audio")) {
            return ContactStatus.RECORDING;
        }

        return node.getAttributeAsString("type")
                .or(() -> node.getChild().map(Node::description))
                .flatMap(ContactStatus::of)
                .orElse(ContactStatus.AVAILABLE);
    }
}
