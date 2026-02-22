package com.arenita.sales.infrastructure.config

import com.arenita.sales.domain.model.*
import com.arenita.sales.domain.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class DataSeeder {

    @Bean
    @Profile("dev")
    fun seedData(companyRepo: CompanyRepository, agentRepo: SalesAgentRepository) = CommandLineRunner {
        if (companyRepo.count() == 0L) {
            val company = companyRepo.save(Company(
                ruc = "0992667273001",
                name = "RAPIDIAGNOSTICS S.A.",
                tradeName = "Rapidiagnostics",
                address = "Av. Juan Tanca Marengo, Mz. 21 S. 5",
                city = "Guayaquil",
                country = "EC",
                industry = Industry.DIAGNOSTICS
            ))

            agentRepo.saveAll(listOf(
                SalesAgent(companyId = company.id!!, name = "Johanna Villamar", role = AgentRole.SALES),
                SalesAgent(companyId = company.id!!, name = "Karen Ruiz", role = AgentRole.ADMIN,
                    telegramId = "5467183402")
            ))
        }
    }
}
