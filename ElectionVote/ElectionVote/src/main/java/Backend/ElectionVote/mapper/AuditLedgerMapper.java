package Backend.ElectionVote.mapper;


import Backend.ElectionVote.dto.AuditLedgerDto;
import Backend.ElectionVote.entity.AuditLedger;

public class AuditLedgerMapper {

    public AuditLedgerDto toDto(AuditLedger a) {
        if (a == null) return null;
        AuditLedgerDto d = new AuditLedgerDto();
        d.setLedgerId(a.getLedgerId());
        d.setEntryType(a.getEntryType());
        d.setEntryReference(a.getEntryReference());
        d.setPayloadHash(a.getPayloadHash());
        d.setPrevHash(a.getPrevHash());
        d.setChainHash(a.getChainHash());
        d.setActorId(a.getActorId());
        d.setSignature(a.getSignature());
        d.setCreatedAt(a.getCreatedAt());
        return d;
    }


    public AuditLedger fromDto(AuditLedgerDto d) {
        if (d == null) return null;
        AuditLedger a = new AuditLedger();
        a.setLedgerId(d.getLedgerId());
        a.setEntryType(d.getEntryType());
        a.setEntryReference(d.getEntryReference());
        a.setPayloadHash(d.getPayloadHash());
        a.setPrevHash(d.getPrevHash());
        a.setChainHash(d.getChainHash());
        a.setActorId(d.getActorId());
        a.setSignature(d.getSignature());
        a.setCreatedAt(d.getCreatedAt());
        return a;
    }
}