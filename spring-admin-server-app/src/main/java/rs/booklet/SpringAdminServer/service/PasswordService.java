package rs.booklet.SpringAdminServer.service;

import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService
{
    private final String hashedPassword;
    private static final String DEFAULT_PASSWORD = "password";
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PasswordService(Environment environment)
    {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProdProfile = false;
        for (String profile : activeProfiles)
        {
            if ("prod".equals(profile))
            {
                isProdProfile = true;
                break;
            }
        }

        String password = environment.getProperty("ADMIN_PASSWORD");
        if (password == null || password.isEmpty())
        {
            if (isProdProfile)
            {
                throw new IllegalStateException(
                    "ADMIN_PASSWORD environment variable must be set when running with 'prod' profile"
                );
            }

            password = DEFAULT_PASSWORD;
        }

        this.hashedPassword = passwordEncoder.encode(password);
    }

    public String getHashedPassword()
    {
        return hashedPassword;
    }
}