package com.poly.models.entities;

import java.util.ArrayList;
import java.util.List;

import com.poly.models.enums.RoleCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name="roles")
public class Role {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)	
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "code")
	private RoleCode code;

	private String name;
	
	@Column(name = "del_if")
	private Boolean deleted;

	@ManyToMany(mappedBy = "roles")
	private List<Account> accounts = new ArrayList<>();
}
