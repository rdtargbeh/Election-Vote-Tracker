package Backend.ElectionVote.service.implement;

import Backend.ElectionVote.dto.ChatReadReceiptDto;
import Backend.ElectionVote.entity.ChatMessage;
import Backend.ElectionVote.entity.ChatReadReceipt;
import Backend.ElectionVote.entity.SystemUser;
import Backend.ElectionVote.mapper.ChatReadReceiptMapper;
import Backend.ElectionVote.repository.ChatMessageRepository;
import Backend.ElectionVote.repository.ChatReadReceiptRepository;
import Backend.ElectionVote.service.ChatReadReceiptService;
import Backend.ElectionVote.utility.ChatReadReceiptId;
import Backend.ElectionVote.utility.TenantUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatReadReceiptServiceImplementation implements ChatReadReceiptService {

    private final ChatReadReceiptRepository receipts;
    private final ChatMessageRepository messages;

    private final ChatReadReceiptMapper mapper = new ChatReadReceiptMapper();

    @Override
    public ChatReadReceiptDto markReadForCurrentUser(UUID messageId) {
        // Tenant guard (ensures you accessed the message via tenant-aware paths elsewhere)
        TenantUtils.requireTenantOrg();

        SystemUser me = currentUserOrThrow();
        ChatMessage msg = messages.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));

        // Upsert pattern via derived finders
        var existing = receipts.findByMessage_MessageIdAndUser_UserId(messageId, me.getUserId());
        ChatReadReceipt r = existing.orElseGet(() -> {
            ChatReadReceipt nr = new ChatReadReceipt();
            nr.setId(new ChatReadReceiptId(messageId, me.getUserId()));
            nr.setMessage(msg);
            nr.setUser(me);
            return nr;
        });

        r.setDateRead(LocalDateTime.now(ZoneOffset.UTC));
        ChatReadReceipt saved = receipts.save(r);
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatReadReceiptDto> listReaders(UUID messageId) {
        TenantUtils.requireTenantOrg();
        List<ChatReadReceipt> list = receipts.findByMessage_MessageId(messageId);
        return list.stream().map(mapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long readerCount(UUID messageId) {
        TenantUtils.requireTenantOrg();
        return receipts.countByMessage_MessageId(messageId);
    }

    @Override
    public int markManyReadForCurrentUser(List<UUID> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return 0;
        TenantUtils.requireTenantOrg();

        SystemUser me = currentUserOrThrow();
        int updated = 0;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // Minimal round-trips; safe derived lookups
        for (UUID mid : messageIds) {
            var msgOpt = messages.findById(mid);
            if (msgOpt.isEmpty()) continue;

            var recOpt = receipts.findByMessage_MessageIdAndUser_UserId(mid, me.getUserId());
            ChatReadReceipt r = recOpt.orElseGet(() -> {
                ChatReadReceipt nr = new ChatReadReceipt();
                nr.setId(new ChatReadReceiptId(mid, me.getUserId()));
                nr.setMessage(msgOpt.get());
                nr.setUser(me);
                return nr;
            });

            r.setDateRead(now);
            receipts.save(r);
            updated++;
        }
        return updated;
    }

    /* ------------ helpers ------------ */

    private SystemUser currentUserOrThrow() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof SystemUser u)) {
            throw new IllegalStateException("Authenticated user required");
        }
        return u;
    }
}
