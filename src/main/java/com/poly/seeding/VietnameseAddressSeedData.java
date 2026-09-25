package com.poly.seeding;

import java.util.List;

/**
 * Delivery addresses use the same three-part format as the checkout form:
 * street address, ward, province/city.
 */
final class VietnameseAddressSeedData {

    private static final List<String> ADDRESSES = List.of(
        "12 Đường Lê Lợi, Phường Bến Nghé, Thành phố Hồ Chí Minh",
        "85 Đường Trần Phú, Phường Lộc Thọ, Tỉnh Khánh Hòa",
        "24 Phố Hàng Bạc, Phường Hàng Bạc, Thành phố Hà Nội",
        "116 Đường Bạch Đằng, Phường Phước Ninh, Thành phố Đà Nẵng",
        "42 Đường Nguyễn Huệ, Phường Vĩnh Ninh, Thành phố Huế",
        "19 Đường Ninh Kiều, Phường Tân An, Thành phố Cần Thơ",
        "68 Đường Hoàng Văn Thụ, Phường Lộc Thọ, Tỉnh Khánh Hòa",
        "33 Đường Lý Tự Trọng, Phường Bến Thành, Thành phố Hồ Chí Minh",
        "91 Phố Huế, Phường Ngô Thì Nhậm, Thành phố Hà Nội",
        "57 Đường Nguyễn Văn Linh, Phường Nam Dương, Thành phố Đà Nẵng",
        "14 Đường Hùng Vương, Phường Phú Hội, Thành phố Huế",
        "73 Đường 30 Tháng 4, Phường An Phú, Thành phố Cần Thơ",
        "28 Đường Võ Thị Sáu, Phường Thống Nhất, Tỉnh Đồng Nai",
        "105 Đường Lê Hồng Phong, Phường 4, Thành phố Vũng Tàu",
        "46 Đường Trần Hưng Đạo, Phường Dương Đông, Tỉnh Kiên Giang",
        "39 Đường Nguyễn Đình Chiểu, Phường 2, Tỉnh Lâm Đồng",
        "81 Đường Quang Trung, Phường Hiệp Ninh, Tỉnh Tây Ninh",
        "22 Đường Nguyễn Tất Thành, Phường Tân Lập, Tỉnh Đắk Lắk",
        "64 Đường Hùng Vương, Phường Lê Lợi, Tỉnh Bình Định",
        "17 Đường Phan Chu Trinh, Phường Điện Biên, Thành phố Đà Nẵng"
    );

    private VietnameseAddressSeedData() {
    }

    static String forSeedNumber(int number) {
        String address = ADDRESSES.get(Math.floorMod(number - 1, ADDRESSES.size()));
        int wardSeparator = address.indexOf(',');
        return "Căn " + String.format("%03d", number) + " - "
            + address.substring(0, wardSeparator)
            + address.substring(wardSeparator);
    }
}
