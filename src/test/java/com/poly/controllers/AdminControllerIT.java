package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.poly.models.entities.Category;
import com.poly.models.repositories.CategoryRepository;

public class AdminControllerIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testFindAllCategories() throws Exception {
        Category category = new Category();
        category.setName("Integration Test Category");
        category.setDeleted(false);
        categoryRepository.save(category);

        var result = mockMvc.perform(get("/admin/category")
                .param("keyword", "Integration Test Category"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Integration Test Category"));
    }
}
