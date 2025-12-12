# CKB Explorer Worker

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

CKB Explorer Worker is a backend task service developed with Spring Boot. It provides data processing tasks for the basic data of Nervos CKB blockchain nodes, and the processed data is available for the CKB Explorer Web API to consume.
## Project Overview

CKB Explorer Worker includes the following core task processing modules, responsible for automated processing and statistics of blockchain data:
- DailyStatistic (Daily data statistics)
- EpochStatistic (Epoch cycle statistics)
- AverageBlockTimeGenerator (Block average time calculation)
- DaoContract (DAO contract data processing)
- ScriptAnalysis (Script information parsing)
- StatisticReset (Statistical data reset)
- UdtDailyStatistic (UDT token daily statistics)


## Technology Stack

- **Backend Framework**: Spring Boot 3.2.10
- **Programming Language**: Java 21
- **Build Tool**: Maven
- **ORM Framework**: MyBatis-Plus
- **Database**: RisingWave (for data analysis and statistics)
- **Caching**: Redis

## Environment Requirements

- JDK 21 or higher
- Maven 3.8+ or higher
- Redis 6.0+ (optional, for caching)
- PostgreSQL 14+ database
- RisingWave database

## Project Structure

```
├── src/
│   ├── main/
│   │   ├── java/             # Java source code
│   │   │   └── com/ckb/explore/    # Main package
│   │   │       ├── enums/          # Data enums
│   │   │       ├── service/         # Business logic layer
│   │   │       ├── mapper/          # Data access layer
│   │   │       ├── domain/          # Data models
│   │   │       ├── config/          # Configuration classes
│   │   │       └── task/            # Data tasks 
│   │   └── resources/        # Configuration files
│   │       ├── application.yml      # Main configuration file
│   │       ├── mapper/              # MyBatis mapping files
│   │       └── static/              # Static resources
│   │       └── *.sql                # RisingWave sql
│   └── test/                # Test code
└── pom.xml                  # Maven configuration file
```

## Configuration Instructions

### Main Configuration File

The main configuration is located in `src/main/resources/application.yml` and includes:

- Server configuration (port, context path, etc.)
- MyBatis-Plus configuration
- Database connection configuration

### Environment-Specific Configuration

The project supports multi-environment configuration:

- `application-testnet.yml`: testnet environment configuration
- `application-mainnet.yml`: Mainnet environment configuration


## Installation and Running

### Building the Project

```bash
mvn clean install
```

### Running the Application

#### Using Maven

```bash
mvn spring-boot:run
```

#### Using the Startup Script

```bash
chmod +x start.sh
./start.sh
```

## Contribution Guidelines

Contributions to this project are welcome! If you want to participate in development, please follow these steps:

1. Fork this repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact

If you have any questions or suggestions, please contact us through:

- Project Repository: [https://github.com/appfi5/ckb-explorer-worker.git](https://github.com/appfi5/ckb-explorer-worker.git)
- Issues: [https://github.com/appfi5/ckb-explorer-worker/issues](https://github.com/appfi5/ckb-explorer-worker/issues)