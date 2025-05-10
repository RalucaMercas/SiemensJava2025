package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        item1 = new Item(1L, "Item 1", "Description 1", "NEW", "item1@example.com");
        item2 = new Item(2L, "Item 2", "Description 2", "NEW", "item2@example.com");
    }

    @Test
    void getAllItems_Success_ReturnsItems() throws Exception {
        List<Item> items = Arrays.asList(item1, item2);
        when(itemService.findAll()).thenReturn(items);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Item 1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Item 2"));

        verify(itemService, times(1)).findAll();
    }

    @Test
    void getAllItems_DatabaseError_ReturnsInternalServerError() throws Exception {
        when(itemService.findAll()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));

        verify(itemService, times(1)).findAll();
    }

    @Test
    void createItem_ValidItem_ReturnsCreated() throws Exception {
        Item newItem = new Item(null, "New Item", "New Desc", "NEW", "new@example.com");
        Item savedItem = new Item(3L, "New Item", "New Desc", "NEW", "new@example.com");

        when(itemService.save(any(Item.class))).thenReturn(savedItem);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.name").value("New Item"));

        verify(itemService, times(1)).save(any(Item.class));
    }

    @Test
    void createItem_ValidItemWithId_IdIsResetAndReturnsCreated() throws Exception {
        Item newItem = new Item(999L, "New Item", "New Desc", "NEW", "new@example.com");
        Item savedItem = new Item(3L, "New Item", "New Desc", "NEW", "new@example.com");

        when(itemService.save(any(Item.class))).thenReturn(savedItem);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.name").value("New Item"));

        verify(itemService, times(1)).save(argThat(item -> item.getId() == null));
    }

    @Test
    void createItem_InvalidItem_ReturnsBadRequest() throws Exception {
        Item invalidItem = new Item(null, "", "", "", "invalid-email");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Name cannot be blank"))
                .andExpect(jsonPath("$.description").value("Description cannot be blank"))
                .andExpect(jsonPath("$.status").value("Status cannot be blank"))
                .andExpect(jsonPath("$.email").value("Email must be a valid"));

        verify(itemService, never()).save(any(Item.class));
    }

    @Test
    void createItem_DatabaseError_ReturnsInternalServerError() throws Exception {
        Item newItem = new Item(null, "New Item", "New Desc", "NEW", "new@example.com");

        when(itemService.save(any(Item.class))).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));

        verify(itemService, times(1)).save(any(Item.class));
    }


    @Test
    void getItemById_ItemExists_ReturnsOk() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.of(item1));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Item 1"));

        verify(itemService, times(1)).findById(1L);
    }

    @Test
    void getItemById_ItemNotFound_ReturnsNotFound() throws Exception {
        when(itemService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/items/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        verify(itemService, times(1)).findById(999L);
    }

    @Test
    void getItemById_DatabaseError_ReturnsInternalServerError() throws Exception {
        when(itemService.findById(1L)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));

        verify(itemService, times(1)).findById(1L);
    }

    @Test
    void updateItem_ItemExists_ReturnsOk() throws Exception {
        Item updatedItem = new Item(null, "Updated Item", "Updated Desc", "UPDATED", "updated@example.com");
        Item savedItem = new Item(1L, "Updated Item", "Updated Desc", "UPDATED", "updated@example.com");

        when(itemService.findById(1L)).thenReturn(Optional.of(item1));
        when(itemService.save(any(Item.class))).thenReturn(savedItem);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Updated Item"));

        verify(itemService, times(1)).findById(1L);
        verify(itemService, times(1)).save(argThat(item -> item.getId().equals(1L)));
    }

    @Test
    void updateItem_ItemNotFound_ReturnsNotFound() throws Exception {
        Item updatedItem = new Item(null, "Updated Item", "Updated Desc", "UPDATED", "updated@example.com");

        when(itemService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/items/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedItem)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        verify(itemService, times(1)).findById(999L);
        verify(itemService, never()).save(any(Item.class));
    }

    @Test
    void updateItem_InvalidItem_ReturnsBadRequest() throws Exception {
        Item invalidItem = new Item(null, "", "", "", "invalid-email");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Name cannot be blank"))
                .andExpect(jsonPath("$.description").value("Description cannot be blank"))
                .andExpect(jsonPath("$.status").value("Status cannot be blank"))
                .andExpect(jsonPath("$.email").value("Email must be a valid"));

        verify(itemService, never()).findById(anyLong());
        verify(itemService, never()).save(any(Item.class));
    }

    @Test
    void updateItem_DatabaseError_ReturnsInternalServerError() throws Exception {
        Item updatedItem = new Item(null, "Updated Item", "Updated Desc", "UPDATED", "updated@example.com");

        when(itemService.findById(1L)).thenReturn(Optional.of(item1));
        when(itemService.save(any(Item.class))).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedItem)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));

        verify(itemService, times(1)).findById(1L);
        verify(itemService, times(1)).save(any(Item.class));
    }

    @Test
    void deleteItem_ItemExists_ReturnsNoContent() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.of(item1));
        doNothing().when(itemService).deleteById(1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(itemService, times(1)).findById(1L);
        verify(itemService, times(1)).deleteById(1L);
    }

    @Test
    void deleteItem_ItemNotFound_ReturnsNotFound() throws Exception {
        when(itemService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/items/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        verify(itemService, times(1)).findById(999L);
        verify(itemService, never()).deleteById(anyLong());
    }

    @Test
    void deleteItem_DatabaseError_ReturnsInternalServerError() throws Exception {
        when(itemService.findById(1L)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));

        verify(itemService, times(1)).findById(1L);
        verify(itemService, never()).deleteById(anyLong());
    }

    @Test
    void processItems_Success_ReturnsOk() throws Exception {
        List<Item> processedItems = Arrays.asList(item1, item2);
        when(itemService.processItemsAsync()).thenReturn(CompletableFuture.completedFuture(processedItems));

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/items/process")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Item 1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Item 2"));

        verify(itemService, times(1)).processItemsAsync();
    }

    @Test
    void processItems_AsyncFailure_ReturnsInternalServerError() throws Exception {
        CompletableFuture<List<Item>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Processing error"));
        when(itemService.processItemsAsync()).thenReturn(failedFuture);

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/items/process")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));

        verify(itemService, times(1)).processItemsAsync();
    }
}