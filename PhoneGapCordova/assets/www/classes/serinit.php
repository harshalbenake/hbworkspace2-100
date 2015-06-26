<?php
   define("MYSQLUSER", "resortcl_app");
    define("MYSQLPASS", "test123");
    define("HOSTNAME", "localhost");
	define("MYSQLDB", "resortcl_status_send");
   
     // Make connection to database
    $connection = @new mysqli(HOSTNAME, MYSQLUSER, MYSQLPASS);
    if ($connection->connect_error) {
      die('Connect Error: ' . $connection->connect_error);
    } else {
    		
	/*//Create the database for storing user information		
    	$createdb = "CREATE DATABASE IF NOT EXISTS status_send";
	if (mysqli_query($connection, $createdb)) {
		echo "";
	} else {
		echo" Error creating status_send Database: " . mysqli_error($connection);
	}
		*/
	//Create a table for the userid
	$con2 = @new mysqli(HOSTNAME, MYSQLUSER, MYSQLPASS, MYSQLDB);
	$createtb1 = "CREATE TABLE IF NOT EXISTS employee(
	id INT NOT NULL AUTO_INCREMENT,
	PRIMARY KEY(id),
	employeeid CHAR(4) DEFAULT '0000')";
	
	if(mysqli_query($con2, $createtb1)) {
		echo "";
	}else{
		echo "Error creating table: " . mysqli_error($con2);
	}
	//Create a table for the sent appointments 	
	$createtb2 = "CREATE TABLE IF NOT EXISTS sent_msg(
	id INT NOT NULL AUTO_INCREMENT,
	PRIMARY KEY(id),
	employee CHAR(14),
	property CHAR(14),
	date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	status int(1),
	status_name CHAR(14)
	)";
	
	if(mysqli_query($con2, $createtb2)) {
		echo "";
	}else{
		echo "Error creating table: " . mysqli_error($con2);
	}}

$insert = "INSERT IGNORE INTO `employee` (`id`, `employeeid`) VALUES "
         . " ('1', '0000')";
        
        // Run the query and display appropriate message
        if (!$result = $con2->query($insert)) {
         echo "Unable to add rows<br />";
        } else {
          echo "";
        }

?>

<?php 

$message = '';
$message1 = '';
$error = '';

if (isset($_POST['save']) AND $_POST['save'] == 'Save') {
  // check the token
  $badToken = true;
  if (empty($_POST['token']) || $_POST['token'] !== $_SESSION['token']) {
    $message = 'Sorry, try it again. There was a security issue.';
    $badToken = true;
  } else {
    $badToken = false;
    unset($_SESSION['token']);

  }}
$set  = 'User ID:';
?>
