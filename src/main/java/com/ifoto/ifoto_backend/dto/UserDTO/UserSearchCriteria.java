package com.ifoto.ifoto_backend.dto.UserDTO;

/**
 * Parameter object bundling the user-listing filter criteria that previously
 * travelled together as separate {@code search} and {@code role} arguments
 * (Data Clumps → Introduce Parameter Object).
 */
public record UserSearchCriteria(String search, String role) {}
