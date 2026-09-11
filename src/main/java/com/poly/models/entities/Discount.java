package com.poly.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "discounts")
public class Discount {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private	String code;
	
	private	BigDecimal percentage;
	
	private String description;
	
	@Column(name = "created_date")
	private LocalDateTime createdDate;
	
	@Column(name = "expired_date")
	private LocalDateTime expiredDate;
	
	private Boolean expired;
	
	@Column(name = "del_if")
	private Boolean deleted;
	
	@ManyToMany(mappedBy = "discounts")
    private List<Product> products = new ArrayList<>();
}
