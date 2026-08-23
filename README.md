![SunClassicLogo](src/main/resources/SunClassic-Logo-Transparent.png)
# SUN Classic
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/Ashime/sun-classic/maven.yml)
[![CodeQL](https://github.com/Ashime/sun-classic/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/Ashime/sun-classic/actions/workflows/github-code-scanning/codeql)
[![Dependabot Updates](https://github.com/Ashime/sun-classic/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/Ashime/sun-classic/actions/workflows/dependabot/dependabot-updates)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/45c4c918abe542449d5673d8720a99e4)](https://app.codacy.com/gh/Ashime/sun-classic/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/Ashime/sun-classic)
![GitHub License](https://img.shields.io/github/license/Ashime/sun-classic)
![GitHub repo size](https://img.shields.io/github/repo-size/Ashime/sun-classic)

> This project developed using the English SUN Classic 2.4.0.2 client. This project has multiple modules inside, such as AuthServer folder contains the AuthServer module. Each module has their own README.md. The license applies to all modules within the project. Each server, Auth, Game, etc. are a Maven project built in IntelliJ IDEA Community.

## ❈ Requirements
➤ Java Development Kit 25<br>
➤ Microsoft SQL Server 2025 Developer<br>
&emsp;&emsp;› SQL Server Management Studio 22<br>
➤ IntelliJ IDEA Community<br>

## ❈ Download and Install
> Everything needs to be downloaded and installed to work on all parts of this project.

### ▣ Java Development Kit 25
1) Download and install <a href = "https://www.oracle.com/java/technologies/downloads/#java25">JDK 25</a>.

### ▣ Microsoft SQL Server 2025 Developer
 1) Download <a href = "https://www.microsoft.com/en-us/sql-server/sql-server-downloads">Microsoft SQL Server 2025 Developer</a>.
 <br><br>
 2) Install SQL Server 2025 Developer.
 <br><b>NOTE</b>: Follow this <u>Windows</u> <a href = "https://learn.microsoft.com/en-us/sql/database-engine/install-windows/install-sql-server?view=sql-server-ver17">installation guide</a> for help unless you are running <u>Linux</u> then follow this <a href = "https://learn.microsoft.com/en-us/sql/linux/sql-server-linux-setup?view=sql-server-ver17">installation guide</a> instead. 
 <br><br>
 3) Download and install <a href = "https://aka.ms/ssms/22/release/vs_SSMS.exe">SQL Server Management Studio 22</a>.

### ▣ IntelliJ IDEA Community
 1) Download <a href = "https://www.jetbrains.com/idea/download/">IntelliJ IDEA Community</a>.
 
 2) Install IntelliJ IDEA. Please follow the Standalone section in this <a href = "https://www.jetbrains.com/help/idea/installation-guide.html#">installation guide</a>.

## ❈ Setup and Configure
> If you plan to <u>contribute</u> to the project then you are required to fork the entire project from the <a href = "https://github.com/Ashime/sun-classic">main directory</a>. Otheriwse, please click the drop-down arrow on the Code button, and click Download as Zip. Unzip "sun-classic-main.zip" in any directory on the computer.

### ▣ Configuring SQL Server 2025 Developer
#### ➤ Restoring Database
1) Open SQL Server Management Studio 22.
2) Login using Windows Authentication under your local instance (e.g. MyComputer).
<br><b>NOTE</b>: Make sure 'Encryption' is set to 'Mandatory' and 'Trust server certificate' is checked under the 'Connection Security' section.
3) Right-click the 'Databases' folder in 'Object Explorer' window on the left and click 'Restore database...'.
4) In the 'Restore Database' window click 'Device' under 'Source' category and click the '...' button on the right to search for 'sun-classic yyyy-mm-dd.bak'.
<br><b>NOTE</b>: Download the latest sun-classic.bak file under the Files section.
5) Once the file is found and selected then click the 'Ok' button at the button right of the 'Restore Database' window.

#### ➤ Creating Login
1) Download the 'Create User Script' from the Files section below.
2) In SSMS press Ctrl + O and search for the 'Create User Script' file.
3) Once open press F5 to execute the script.

#### ➤ Setup SSMS Multi-Login
1) Right-click the server name (e.g. MyComputer) in Object Explorer and click Properties.
2) Go to Security on the left in the new Window and select 'SQL Server and Windows Authentication mode' under 'Server authentication' section.
3) Click OK button at the bottom.

#### ➤ Configure SQL Server Configuration Manager
1) Open SQL Server Configuration Manager and expand SQL Server Network Configuration section.
2) Click Protocols for MSSQLSERVER, right-click 'TCP/IP' on the right, and click Enable.
3) Double click 'TCP/IP', click the 'IP Addresses' tab in the new window, and scroll down until you see '127.0.0.1' in the IP Address row.
4) Change Enabled to Yes, clear the TCP Dynamic Ports from 0 to blank, and type in 1433 under TCP Port.
5) Scroll to 'IPAll' section at the bottom, clear 0 to blank in the TCP Dynamic Ports, and type in 1433 under TCP Port. Click OK button.
6) Click 'SQL Server Services' on the left and right-click 'SQL Server (MSSQLSERVER)' on the right, and click 'Restart'.

### ▣ Configuring IntelliJ IDEA Community
#### ➤ Open Projects
1) If no projects are currently open, then IntelliJ launcher will appear, click "Open or Import" on the right hand side of the window.
<br><b>NOTE</b>: Otherwise, do File->Open and search for the project.

2) Search of the project directory where you unzipped SUN Classic, and open the 'sun-classic' project.
<br><b>WARNING</b>: Opening each module separately will cause multiple dependency errors.

#### ➤ Selecting Project SDK
1) With the project open, go to File, and click 'Project Structure...'.
2) In the Project Structure window, and click the Project tab inside the Project Settings section on the left.
3) In the middle section of the window, under the Project SDK section, click the drop-down menu, and select 'java version 25'.
4) Click the OK button at the bottom of the window.

## ❈ Files
<b>SUN Classic Database - Backup (2025-02-16)</b>
> Google Drive
> <br> <b>Link</b>: <https://drive.google.com/file/d/1Y0vrwFyoqEfJyZhD-8qeo85ajmzpCjV5/view?usp=sharing>.

<b>SUN Classic Database - Create User Script (2025-02-23)</b>
> Google Drive
> <br> <b>Link</b>: <https://drive.google.com/file/d/1jNri6MUt4UKfEUmiRk2_cUrBc175XTeQ/view?usp=sharing>.

<b>SUN Classic Client v2.4.0.2 (2024-07-17)</b>
> Google Drive
> <br> <b>Link</b>: <https://drive.google.com/file/d/1aD1jsEetGqjmwSwpboii4yg8Umm6WdjH/view?usp=sharing>
