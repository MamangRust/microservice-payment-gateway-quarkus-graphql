package com.sanedge.user.domain.response;

import com.sanedge.user.entity.User;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@RegisterForReflection
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDeleteAt {
  private Integer id;
  private String username;
  private String firstname;
  private String lastname;
  private String email;
  private String createdAt;
  private String updatedAt;
  private String deletedAt;

  public static UserResponseDeleteAt from(User user) {
    return UserResponseDeleteAt.builder()
        .id(user.id.intValue())
        .username(user.getUsername())
        .firstname(user.getFirstname())
        .lastname(user.getLastname())
        .email(user.getEmail())
        .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
        .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
        .deletedAt(user.getDeletedAt() != null ? user.getDeletedAt().toString() : null)
        .build();
  }
}
