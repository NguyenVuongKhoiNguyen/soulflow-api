package com.poly.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.poly.models.enums.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
@Table(name="orders")
public class Order {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String fullname;

	@Column(name = "phone_number")
    private String phone;
    
    private String address;
    
	@Setter(AccessLevel.NONE)
	private BigDecimal total;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "created_date")
	private LocalDateTime createdDate;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "expired_date")
	private LocalDateTime expiredDate;

	private Boolean expired;

	@Enumerated(EnumType.STRING)
    @Column(insertable = false)
	private OrderStatus status;

	@Column(name = "del_if")
	private Boolean deleted;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
    
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails;

	@OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Payment payment;

	public void calTotal() {
		BigDecimal temp = BigDecimal.ZERO;
		if (orderDetails == null) {
			total = temp;
			return;
		}
		for (OrderDetail od : orderDetails) {
			if (od == null || od.getSubtotal() == null) {
				continue;
			}
			temp = temp.add(od.getSubtotal());
		}
		total = temp;
	}
	
	/*
		CascadeType determines whether an something run on the parrent should run on it children too.
		FetchType determines when loading the parent should the children also be loaded too.
	*/

	/*
	 the cascate type:
		ALL
		PERSIST: insert both parents and children
		MERGE: update both parents and children
		REMOVE: remove both parent and children
		REFRESH: refresh both
		DETATCH: 
	 */

	/*
		there are two fetch types:
		LAZY: only call db when children get access
		EAGER: load parent and children at the same time
	*/
}
