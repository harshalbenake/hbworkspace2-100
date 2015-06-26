<?php 
session_start();
include_once 'classes/init.php';
include_once 'classes/saveappt.php';
define('MAGIC_QUOTES_ACTIVE', get_magic_quotes_gpc()); 
?>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title></title>
</head>
<body>
    <p><a href="settings.php">Settings</a> | <a href="history.php">History</a> | <a href="index.php">Update Status</a></p>
<h1><?php echo $set; ?><?php   $eid = mysqli_query($con3, "SELECT employeeid FROM employee ORDER BY id DESC LIMIT 1");
 while ($row = $eid->fetch_assoc()){
	echo $row['employeeid'];
 } ?></h1>

<p><?php echo $message; ?></p>

<form action="index.php" method="post" name="maint" id="maint">

  <fieldset class="maintform">
    <legend>Enter Your Appointment ID</legend>
    <ul>
      <li><label for="appt">Appointment ID</label><br />
        <input type="text" name="appt" id="appt" maxlength="5" pattern=".{5,}" required/></li>
    </ul>

    <?php 
    // create token
    $salt = 'SomeSalt';
    $token = sha1(mt_rand(1,1000000) . $salt); 
    $_SESSION['token'] = $token;
    ?>
    <input type='hidden' name='token' value='<?php echo $token; ?>'/>
    <input type='hidden' name='eid' value='<?php 
    $eid = mysqli_query($con3, "SELECT employeeid FROM employee ORDER BY id DESC LIMIT 1");
 while ($row = $eid->fetch_assoc()){
	echo $row['employeeid'];}  ?>'/>
    
<button type="submit" name="checkin">Check In</button>
<button type="submit" name="checkout">Check Out</button>
<button type="submit" name="qccheck">Qualiy Check</button>
    </fieldset>
</form>

</body>
</html>