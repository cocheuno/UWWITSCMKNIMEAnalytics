# UWW ITSCM KNIME Analytics - K-Means Clustering Node

[![License](https://img.shields. io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![KNIME](https://img.shields.io/badge/KNIME-5.8-yellow. svg)](https://www.knime.com/)

A custom KNIME Analytics Platform node that performs K-Means clustering analysis on datasets.  Developed by UWW ITSCM (University of Wisconsin-Whitewater IT Supply Chain Management). 

## Overview

This extension provides a K-Means clustering node for the KNIME Analytics Platform, enabling users to perform unsupervised machine learning clustering operations directly within their KNIME workflows.

### Features

- **K-Means Clustering Algorithm**: Partition data into k distinct clusters based on feature similarity
- **Configurable Parameters**: Set the number of clusters and other algorithm parameters
- **Seamless Integration**: Works with standard KNIME data tables
- **Visual Output**: View clustering results and assignments

## Requirements

- **KNIME Analytics Platform**: Version 5.8 or higher
- **Java**: JDK 17 or higher (for development)
- **Maven**: 3.6+ (for building from source)

## Installation

### From Update Site

1. Open KNIME Analytics Platform
2. Go to **File** → **Install KNIME Extensions.. .**
3. Click **Add... ** to add a new update site
4. Enter the update site URL: `[Your Update Site URL]`
5. Select **UWW KMeans Clustering** from the list
6. Follow the installation wizard and restart KNIME

### From Source

1. Clone the repository:
   ```bash
   git clone https://github.com/cocheuno/UWWITSCMKNIMEAnalytics.git
   cd UWWITSCMKNIMEAnalytics/org.uwwitscm.analytics
   ```

2. Build with Maven:
   ```bash
   mvn clean verify
   ```

3. The update site will be generated in `kmeans/site/target/repository/`

4. In KNIME, add the local update site and install the extension

## Usage

1. After installation, find the **UWW KMeans** node in the Node Repository under:
   ```
   Community → UWW Analytics
   ```

2.  Drag the node onto your workflow canvas

3. Connect your input data table to the node's input port

4. Configure the node:
   - Double-click the node to open the configuration dialog
   - Set the number of clusters (k)
   - Select the columns to use for clustering
   - Configure additional parameters as needed

5. Execute the node to perform clustering

### Example Workflow

```
[Data Reader] → [Column Filter] → [UWW KMeans] → [Color Manager] → [Scatter Plot]
```

## Project Structure

```
org.uwwitscm.analytics/
├── kmeans/
│   ├── plugin/          # Main plugin source code
│   │   ├── src/         # Java source files
│   │   ├── icons/       # Node icons
│   │   ├── lib/         # Dependencies
│   │   └── plugin.xml   # KNIME extension point registration
│   ├── feature/         # Eclipse feature definition
│   └── site/            # P2 update site configuration
├── META-INF/
└── pom.xml              # Parent Maven POM
```

## Development

### Prerequisites

- Eclipse IDE with PDE (Plugin Development Environment)
- KNIME SDK
- Maven 3.6+
- JDK 17

### Setting Up Development Environment

1. Import the project into Eclipse as existing Maven projects
2. Set up the target platform to include KNIME Analytics Platform 5.8
3. Run as Eclipse Application to test

### Building

```bash
cd org.uwwitscm.analytics
mvn clean verify
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Author

**UWW ITSCM** - University of Wisconsin-Whitewater, IT Supply Chain Management

## Support

- **Issues**: Please report bugs and feature requests via [GitHub Issues](https://github.com/cocheuno/UWWITSCMKNIMEAnalytics/issues)
- **Documentation**: [KNIME Documentation](https://docs. knime.com/)

## Acknowledgments

- [KNIME AG](https://www.knime. com/) for the KNIME Analytics Platform
- The KNIME Community for resources and examples

---

**Note**: This extension is a community contribution and is not officially supported by KNIME AG. 
