package com.siemens.internship;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    // this worked fine before, but I added a try-catch in case something goes wrong,
    // for example, a database error
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        try {
            List<Item> items = itemService.findAll();
            return new ResponseEntity<>(items, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // http status codes were reversed here
    // if "result" has errors, then the user sent invalid data (checked by @Valid)
    // so we return BAD_REQUEST, otherwise we return CREATED
    // since we use @Valid, but there were no annotations on the fields in Item class, I added some
    // if some data is invalid, we return a map with the errors
    // and if the data is valid, but the item cannot be saved into the db, we return an INTERNAL_SERVER_ERROR
    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {
        ResponseEntity<?> errors = getResponseEntity(result);
        if (errors != null)
            return errors;

        // if the id is specified in the JSON request, and that id already exists in the db,
        // it shouldn't override existing data; to fix this, I set the id to null to make sure
        // the id is auto-generated and no overriding happens
        if (item.getId() != null) {
            item.setId(null);
        }

        try {
            Item savedItem = itemService.save(item);
            return new ResponseEntity<>(savedItem, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // replaced NO_CONTENT with NOT_FOUND because it seems more appropriate
    // NO_CONTENT means that the request was successful, but there is nothing to return,
    // but if the id doesn't exist, it means that the request was not successful
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        try {
            return itemService.findById(id)
                    .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // added validations just like for createItem
    // made sure the updated item is returned in case of success
    // in case the id doesn't exist, we return NOT_FOUND instead of ACCEPTED
    // and OK instead of CREATED in case of success
    // prevented database errors with a try-catch
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @Valid @RequestBody Item item, BindingResult result) {
        ResponseEntity<?> errors = getResponseEntity(result);
        if (errors != null)
            return errors;
        try {
            Optional<Item> existingItem = itemService.findById(id);
            if (existingItem.isPresent()) {
                item.setId(id);
                Item updatedItem = itemService.save(item);
                return new ResponseEntity<>(updatedItem, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<?> getResponseEntity(BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : result.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }
        return null;
    }

    // check if the id exists in the db before deleting
    // if it does, delete it and return NO_CONTENT instead of HttpStatus.CONFLICT
    // if it doesn't, return NOT_FOUND
    // added a try-catch to prevent database errors
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        try {
            Optional<Item> existingItem = itemService.findById(id);
            if (existingItem.isPresent()) {
                itemService.deleteById(id);
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/process")
    public CompletableFuture<ResponseEntity<List<Item>>> processItems() {
        return itemService.processItemsAsync()
                .thenApply(items -> new ResponseEntity<>(items, HttpStatus.OK))
                .exceptionally(ex -> new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));
    }
}