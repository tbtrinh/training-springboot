package com.taibtrinh.cashcard;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;

@RestController
@RequestMapping("/cashcards")
public class CashCardController
{
    private final CashCardRepository cashCardRepository;

    public CashCardController(CashCardRepository cashCardRepository)
    {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping()
    public ResponseEntity<Collection<CashCard>> findAll(Pageable pageable,
                                                        Principal principal)
    {
        Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
                )
        );
        return ResponseEntity.ok(page.toList());
    }

    @GetMapping("/{requestedId}")
    public ResponseEntity<CashCard> findById(@PathVariable Long requestedId,
                                             Principal principal)
    {
        return findCashCard(requestedId, principal)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest,
                                               UriComponentsBuilder ubc,
                                               Principal principal)
    {
        CashCard savedCashCard = cashCardRepository.save(new CashCard(null, newCashCardRequest.amount(), principal.getName()));
        URI locationOfNewCashCard = ubc
                .path("cashcards/{id}")
                .buildAndExpand(savedCashCard.id())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @PutMapping("/{requestedId}")
    public ResponseEntity<Void> putCashCard(@PathVariable Long requestedId,
                                            @RequestBody CashCard cashCardUpdate,
                                            Principal principal)
    {
        Optional<CashCard> cashCardOptional = findCashCard(requestedId, principal);
        if (cashCardOptional.isPresent())
        {
            CashCard cashCard = cashCardOptional.get();
            cashCardRepository.save(new CashCard(cashCard.id(), cashCardUpdate.amount(), principal.getName()));
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{requestedId}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long requestedId,
                                                Principal principal)
    {
        if (cashCardRepository.existsByIdAndOwner(requestedId, principal.getName()))
        {
            cashCardRepository.deleteById(requestedId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private Optional<CashCard> findCashCard(Long requestedId, Principal principal)
    {
        return cashCardRepository.findByIdAndOwner(requestedId, principal.getName());
    }
}
