![SunClassicLogo](src/main/resources/SunClassic-Logo-Transparent.png)
# SUN Classic
> This project developed using the English SUN Classic 2.4.0.2 client. This project has multiple modules inside, such as LoginServer folder contains the LoginServer module. Each module has their own README.md. The license applies to all modules within the project. Each server, Login, Game, etc. are a Maven project built in IntelliJ IDEA Community.

## ❈ Requirements
➤ Java Development Kit 17<br>
➤ Microsoft SQL Server 2022 Developer<br>
&emsp;&emsp;› SQL Server Management Studio 20.2<br>
➤ IntelliJ IDEA Community<br>

## ❈ Download and Install
> Everything needs to be downloaded and installed to work on all parts of this project.

### ▣ Java Development Kit 17
1) Download and install <a href = "https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html">JDK 17</a>.

### ▣ Microsoft SQL Server 2022 Developer
 1) Download <a href = "https://www.microsoft.com/en-us/sql-server/sql-server-downloads">Microsoft SQL Server 2022 Developer</a>.
 <br><b>NOTE:</b> For <u>Windows</u>: Under the "Or, download a free specialized edition" section click "Download now" for the Developer Edition. For <u>Linux</u>: Scroll down the page and click "Choose your installation setup >" under Linux.
 <br><br>
 2) Install SQL Server 2022 Developer.
 <br><b>NOTE</b>: Follow this <u>Windows</u> <a href = "https://www.mssqltips.com/sqlservertip/7313/install-sql-server-2022/">installation guide</a> for help unless you are running <u>Linux</u> then follow this <a href = "https://learn.microsoft.com/en-us/sql/linux/sql-server-linux-setup-machine-learning-sql-2022?view=sql-server-ver16">installation guide</a> instead. 
 <br><br>
 3) Download and install <a href = "https://learn.microsoft.com/en-us/sql/ssms/download-sql-server-management-studio-ssms?view=sql-server-ver16#download-ssms">SQL Server Management Studio 20.2</a>.

### ▣ IntelliJ IDEA Community
 1) Download <a href = "https://www.jetbrains.com/idea/download/">IntelliJ IDEA Community</a>.
 
 2) Install IntelliJ IDEA. Please follow the Standalone section in this <a href = "https://www.jetbrains.com/help/idea/installation-guide.html#">installation guide</a>.

## ❈ Setup and Configure
> Recommended to download the entire project from the <a href = "https://github.com/Ashime/sun-classic">main directory</a>. Click the drop-down arrow on the Code button, and click Download as Zip. Unzip "sun-classic-main.zip" in any directory on the computer.

### ▣ Configuring SQL Server 2022 Developer
#### ➤ Creating Database and Login
1) Open SQL Server Management Studio 20.2.
2) Login using Windows Authentication under your local instance (e.g. MyComputer/SQLEXPRESS).
3) Go to File > Open > File... or Ctrl + O and open the "SunClassic.sql" script.
<br><b>NOTE</b>: Download the script under the Files section.
4) Execute (F5) the script.

#### ➤ Setup SSMS Multi-Login
1) Right-click the server name (e.g. MyComputer/SQLEXPRESS) in Object Explorer and click Properties.
2) Go to Security on the left in the new Window and select 'SQL Server and Windows Authentication mode' under 'Server authentication' section.
3) Click OK button at the bottom.

#### ➤ Configure Sql Server Configuration Manager
1) Open Sql Server Configuration Manager and expand SQL Server Network Configuration section.
2) Click Protocols for SQLEXPRESS, right-click 'TCP/IP' on the right, and click Enable.
3) Double click 'TCP/IP', click the 'IP Addresses' tab in the new window, and scroll down until you see '127.0.0.1' in the IP Address row.
4) Change Active to Yes, clear the TCP Dynamic Ports from 0 to blank, and type in 1433 under TCP Port.
5) Scroll to 'IPAll' section at the bottom, clear 0 to blank in the TCP Dynamic Ports, and type in 1433 under TCP Port.
6) Click OK button.

### ▣ Configuring IntelliJ IDEA Community
#### ➤ Open Projects
1) If no projects are currently open, then IntelliJ launcher will appear, click "Open or Import" on the right hand side of the window.
<br><b>NOTE</b>: Otherwise, do File->Open and search for the project.

2) Search of the project directory where you unzipped SUN Classic, and open the 'sun-classic' project.
<br><b>WARNING</b>: Opening each module separately will cause multiple dependency errors.

#### ➤ Selecting Project SDK
1) With the project open, go to File, and click 'Project Structure...'.
2) In the Project Structure window, and click the Project tab inside the Project Settings section on the left.
3) In the middle section of the window, under the Project SDK section, click the drop-down menu, and select 'java version 17'.
4) Click the OK button at the bottom of the window.

## ❈ Files
<b>SUN Classic Database - Script (2024-08-21)</b>
> Google Drive
> <br> <b>Link</b>: <https://drive.google.com/file/d/1MHP66wIytDl_fzwisNHftRgJiSh41wok/view?usp=sharing>.

<b>SUN Classic Client v2.4.0.2 (2024-07-17)</b>
> Google Drive
> <br> <b>Link</b>: <https://drive.google.com/file/d/1aD1jsEetGqjmwSwpboii4yg8Umm6WdjH/view?usp=sharing>
