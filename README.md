# SUN Classic
> This project developed using the English SUN Classic 2.3.0.1 client. This project has multiple projects inside, such as LoginServer folder has the LoginServer project. Each project has their own README.md. The license in the main directory applies to all projects. Each server, Login, Game, etc. are a Maven project built in IntelliJ IDEA Community.

## ❈ Requirements
- Java Development Kit 17
- Microsoft SQL Server 2022 Express
- IntelliJ IDEA Community

## ❈ Download and Install
> Everything needs to be downloaded and installed to work on all parts of this project.

### ▣ Java Development Kit 17
1) Download and install <a href = "https://www.oracle.com/java/technologies/javase-jdk15-downloads.html">JDK 15</a>.

### ▣ Microsoft SQL Server 2022 Express
 1) Download <a href = "https://go.microsoft.com/fwlink/p/?linkid=2216019&clcid=0x409&culture=en-us&country=us">Microsoft SQL Server 2022 Express</a>.
 <br><b>NOTE:</b> For Windows, click on 'MySQL Installer for Windows', otherwise, click on 'MySQL Community Server'.
 
 2) Install MySQL Community. Please follow this <a href = "https://dev.mysql.com/doc/mysql-getting-started/en/#mysql-getting-started-installing">installation guide</a> for help.

### ▣ IntelliJ IDEA Community
 1) Download <a href = "https://www.jetbrains.com/idea/download/">IntelliJ IDEA Community</a>.
 
 2) Install IntelliJ IDEA. Please follow the Standalone section in this <a href = "https://www.jetbrains.com/help/idea/installation-guide.html#">installation guide</a>.

### ▣ Visual Studio Community 2019
 1) Download <a href = "https://visualstudio.microsoft.com/">Visual Studio Community</a>.
 <br><b>NOTE</b>: Mac users will have to download Visual Studio for macOS.
 
 2) Install Visual Studio Community 2019. Please follow this <a href ="https://docs.microsoft.com/en-us/visualstudio/install/install-visual-studio?view=vs-2019">installation guide</a> for help.
 <br><b>NOTE</b>: Visual Studio's '.NET desktop development' package is the recommended package to install. This package comes with C#.

## ❈ Setting up and Configuring
> You will have to download the entire main directory (master), since the project is built in multiple languages and do not work together as one whole project. At the <a href = "https://github.com/Ashime/AionServer">main directory</a>, click the drop-down arrow on the Code button, and click Download as Zip. Unzip "AionServer-master.zip" in any directory on the computer.

### ▣ Configuring MySQL Community Server 8
1) Open MySQL Workbench 8.
<br><b>NOTE:</b> For users using command line, please follow this <a href = "https://phoenixnap.com/kb/how-to-backup-restore-a-mysql-database">guide</a> to restore the database.

2) Double-click on your 'Local instance MySQL80' and enter your root password. The root password would have been created during installation.
<br><b>NOTE:</b> To reset your root password on Windows, follow this <a href = "https://dev.mysql.com/doc/mysql-windows-excerpt/8.0/en/resetting-permissions-windows.html">guide</a>. For Linux users, please follow this <a href = "https://tecadmin.net/how-to-recover-mysql-root-password/">guide</a>.

3) In the Navigator window, click the Administrator tab at the bottom of the window, and click 'Data Import/Restore'.

4) In the 'Import Options' section, select 'Import from Self-Contained File', and browse for the 'aion.sql' file.
<br><b>NOTE:</b> You need to download the 'Aion Database' zip file from the link in the File section below.

5) At the bottom, click 'Start Import' button.
<br><b>NOTE:</b> Once the import is complete, then you can close the tab.

6) In the Navigator menu, click the Schemas tab at the bottom, click the refresh button near the top, and 'aion' database should appear.

### ▣ Configuring IntelliJ IDEA Community
#### ➤ Open Projects
1) If no projects are currently open, then IntelliJ launcher will appear, click "Open or Import" on the right hand side of the window.
<br><b>NOTE</b>: Otherwise, do File->Open and search for the project.

2) Search of the project directory where you unzipped the AionServer at, and open the LoginServer project.

#### ➤ Selecting Project SDK
1) With the project open, go to File, and click 'Project Structure...'.

2) In the Project Structure window, and click the Project tab inside the Project Settings section on the left.

3) In the middle section of the window, under the Project SDK section, click the drop-down menu, and select 'java version 15'.

4) Click the OK button at the bottom of the window.

#### ➤ Configure Hibernate
1) With the project open, expand the project folders, and find src -> main -> resources.

2) In the "resources" folder, open hibernate.cfg.xml.

3) Find the property tag named "connection.username" and change the value to the MySQL username.
<br><b>NOTE</b>: Default is root.

4) Find the property tag named "connection.password" and change the value to the username's password.
<br><b>NOTE</b>: If the username is root, then the password was set up during the installation process.

## ❈ Files
<b>Aion Database (N/A)</b>
> The tables are generated by Hibernate. 
> <br> <b>Link</b>: TBA at later date.

<b>Aion Client v7.7 (09/10/2020)</b>
> The link below directs you to the retail website. Download the NCLauncher2, login (account required), and download the client as normal. In the future, the client will be mirrored on another website.
> <br> <b>Link</b>: <https://www.aiononline.com/download>
