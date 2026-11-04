package com.admaterial.vo;

import com.admaterial.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVO {
    private Long id;
    private String customerName;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String notes;

    public static CustomerVO from(Customer c) {
        if (c == null) return null;
        return new CustomerVO(
                c.getId(), c.getCustomerName(), c.getContactPerson(),
                c.getContactPhone(), c.getContactEmail(), c.getAddress(), c.getNotes()
        );
    }
}
