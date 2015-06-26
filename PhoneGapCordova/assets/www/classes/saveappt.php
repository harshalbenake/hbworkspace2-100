<?php

//Save the appointment


date_default_timezone_set('America/Chicago');

	  $con3 = @new mysqli(HOSTNAME, MYSQLUSER, MYSQLPASS, MYSQLDB);
      // Get the data
        
        if(isset($_POST['checkin'])){
        $appt = $_POST['appt'];
		$empid = $_POST['eid'];
       
	    // Set up the query 
        $date = date('Y-m-d g:i:s');	
        $query = "INSERT INTO `sent_msg` (`id`, `employee`, `property`, `date`, `status`, `status_name`) VALUES "
         . " ('', '$empid', '$appt', '$date', '1', 'check-in')";
        
        // Run the query and display appropriate message
        if (!$result = $con3->query($query)) {
          $message1 .= "Unable to add rows<br />";
        } else {
          $message1 .=  '<div class="alert alert-dismissable alert-danger"><button data-dismiss="alert" class="close" type="button">×</button><h4 align="center">You have Checked-In</h4><p align="center">To Appointment</br><b> ' . $appt . '</b> on <b>' . $date . '</b></p>';
        }
}else{
	if(isset($_POST['checkout'])){
        $appt = $_POST['appt'];
		$empid = $_POST['eid'];
       
	    // Set up the query 
        $date = date('Y-m-d g:i:s');	
        $query = "INSERT INTO `sent_msg` (`id`, `employee`, `property`, `date`, `status`, `status_name`) VALUES "
         . " ('', '$empid', '$appt', '$date', '3', 'check-out')";
        
        // Run the query and display appropriate message
        if (!$result = $con3->query($query)) {
         $message1 .= "Unable to add rows<br />";
        } else {
          $message1 .=  '<div class="alert alert-dismissable alert-success"><button data-dismiss="alert" class="close" type="button">×</button><h4 align="center">You have Checked-Out</h4><p align="center">of Appointment</br><b> ' . $appt . '</b> on <b>' . $date . '</b></p>';
        }
	
	
}else{
		if(isset($_POST['qccheck'])){
        $appt = $_POST['appt'];
		$empid = $_POST['eid'];
       
	    // Set up the query 
        $date = date('Y-m-d g:i:s');	
        $query = "INSERT INTO `sent_msg` (`id`, `employee`, `property`, `date`, `status`, `status_name`) VALUES "
         . " ('', '$empid', '$appt', '$date', '5', 'qc-check')";
        
        // Run the query and display appropriate message
        if (!$result = $con3->query($query)) {
         $message1 .= "Unable to add rows<br />";
        } else {
          $message1 .=  '<div class="alert alert-dismissable alert-info"><button data-dismiss="alert" class="close" type="button">×</button><h4 align="center">Quality Control Check Complete</h4><p align="center">for Appointment</br><b> ' . $appt . '</b> on <b>' . $date . '</b></p>';
        }
	
}else{
		
	 if(isset($_POST['saveid'])){	
//Save the employee ID
	 $code = $_POST['empid'];
	  $con3 = @new mysqli(HOSTNAME, MYSQLUSER, MYSQLPASS, MYSQLDB);
      // Get the data
       
        // Set up the query 
        $query = "INSERT INTO `employee` (`id`, `employeeid`) VALUES "
         . " ('', '$code')";
        
        // Run the query and display appropriate message
        if (!$result = $con3->query($query)) {
          $message1 .= "Unable to update user ID<br />";
        } else {
 		    $message1 .=  '<div class="alert alert-dismissable alert-success"><button data-dismiss="alert" class="close" type="button">×</button><h4 align="center">User ID Update Successful!</h4><p align="center">Your User ID is</br><b> ' . $code . '</b></p>';

        }
 }}} {
		
	
		
	
}

}
//Display the employee Id


?>