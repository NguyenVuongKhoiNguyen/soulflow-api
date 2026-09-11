package com.poly.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name="carts")

public class Cart {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "created_date")
	private LocalDateTime createdDate; 
	
	@DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "expired_date")
	private LocalDateTime expiredDate;

	private Boolean expired;

	@Setter(AccessLevel.NONE)
	private BigDecimal total;

	@Column(name = "del_if")
	private Boolean deleted;

	@ManyToOne
	@JoinColumn(name = "account_id")
	private Account account;
	
	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL , fetch = FetchType.LAZY)
	private List<Item> items = new ArrayList<>();
	
	public void calTotal() {
		BigDecimal temp = BigDecimal.ZERO;
		for (Item it : items) {
			temp = temp.add(it.getSubtotal());
		}
		total = temp;
	}
}
