package com.sanedge.role.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
public class UserRole extends PanacheEntityBase implements Serializable {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}
