package br.com.jylabtech.stone_payment_tech.licence;

import java.util.Date;

public interface LicenceClock {
    Date now();

    LicenceClock SYSTEM = new LicenceClock() {
        @Override
        public Date now() {
            return new Date();
        }
    };
}
