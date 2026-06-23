package com.monteweb.user;

import com.monteweb.TestContainerConfig;
import com.monteweb.user.internal.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainerConfig.class)
class UserServiceFindByRoleTest {

    @Autowired
    private UserService userService;

    @Test
    void findByRole_returnsActiveSuperadmins() {
        List<UserInfo> admins = userService.findByRole(UserRole.SUPERADMIN);
        assertThat(admins).isNotNull();
        assertThat(admins).isNotEmpty();
        assertThat(admins).allSatisfy(u -> assertThat(u.role()).isEqualTo(UserRole.SUPERADMIN));
    }
}
