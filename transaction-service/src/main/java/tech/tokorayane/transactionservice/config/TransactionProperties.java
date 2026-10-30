package tech.tokorayane.transactionservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "transaction")
@Getter
@Setter
public class TransactionProperties {

    private BigDecimal maxAmount = new BigDecimal("50000");
}
