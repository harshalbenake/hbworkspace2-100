<?php 
session_start();
include_once 'classes/init.php';
include_once 'classes/saveappt.php';
define('MAGIC_QUOTES_ACTIVE', get_magic_quotes_gpc()); 
?>
<html>
<head>
	<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - Dark Admin</title>

    <link rel="stylesheet" type="text/css" href="bootstrap/css/bootstrap.min.css" />
    <link rel="stylesheet" type="text/css" href="font-awesome/css/font-awesome.min.css" />
    <link rel="stylesheet" type="text/css" href="css/local.css" />

    <script type="text/javascript" src="js/jquery-1.10.2.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>

    <!-- you need to include the shieldui css and js assets in order for the charts to work -->
    <link rel="stylesheet" type="text/css" href="http://www.shieldui.com/shared/components/latest/css/shieldui-all.min.css" />
    <link rel="stylesheet" type="text/css" href="http://www.shieldui.com/shared/components/latest/css/light-bootstrap/all.min.css" />
    <link id="gridcss" rel="stylesheet" type="text/css" href="http://www.shieldui.com/shared/components/latest/css/dark-bootstrap/all.min.css" />

    <script type="text/javascript" src="http://www.shieldui.com/shared/components/latest/js/shieldui-all.min.js"></script>
    <script type="text/javascript" src="http://www.prepbootstrap.com/Content/js/gridData.js"></script>
</head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title></title>
</head>
<body>
	 <div id="wrapper">

          <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-ex1-collapse">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="index.html"><img src="classes/logo.png" width="150px"/></a>
            </div>

            <div class="collapse navbar-collapse navbar-ex1-collapse">
                <ul id="active" class="nav navbar-nav side-nav">
                    <li><a href="index.php"><i class="fa fa-bullseye"></i> Update Status</a></li>
                    <li><a href="history.php"><i class="fa fa-table"></i> History</a></li>
                    <li class="selected"><a href="settings.php"><i class="fa fa-cogs"></i> Settings</a></li>
                </ul>
            </div>
        </nav>
	<div id="page-wrapper">

            <div class="row">
                <div class="col-lg-12">
                     <p></p>
<h1 align="center">Your Current</br><small>User ID: <?php   $eid = mysqli_query($con3, "SELECT employeeid FROM employee ORDER BY id DESC LIMIT 1");
														 while ($row = $eid->fetch_assoc()){
															echo $row['employeeid'];
														 } ?></small></h1>

<p align="center">To update your user ID, Enter your 4 digit User ID and click Save. 
<form action="index.php" method="post" name="maint" id="maint">
 <table align="center" width="150px"><tr><td>
	        <input class="form-control" text" name="empid" id="empid" maxlength="4" pattern=".{4,}" required/>
	        </td></tr></table>


	    <?php 
	    // create token
	    $salt = 'SomeSalt';
	    $token = sha1(mt_rand(1,1000000) . $salt); 
	    $_SESSION['token'] = $token;
	    ?>
	    <input type='hidden' name='token' value='<?php echo $token; ?>'/></br>
		<p align="center"><button class="btn btn-danger" name="checkin" a href="index.php">Cancel</button></br></br>
		<button type="submit" class="btn btn-success btn-lg" name="saveid">Save</button></p>
</form>
</div>

                </div>
            </div>
        </div>

    </div>
    <!-- /#wrapper -->
    <script type="text/javascript">
        jQuery(function ($) {
            var performance = [12, 43, 34, 22, 12, 33, 4, 17, 22, 34, 54, 67],
                visits = [123, 323, 443, 32],
                traffic = [
                {
                    Source: "Direct", Amount: 323, Change: 53, Percent: 23, Target: 600
                },
                {
                    Source: "Refer", Amount: 345, Change: 34, Percent: 45, Target: 567
                },
                {
                    Source: "Social", Amount: 567, Change: 67, Percent: 23, Target: 456
                },
                {
                    Source: "Search", Amount: 234, Change: 23, Percent: 56, Target: 890
                },
                {
                    Source: "Internal", Amount: 111, Change: 78, Percent: 12, Target: 345
                }];


            $("#shieldui-chart1").shieldChart({
                theme: "dark",

                primaryHeader: {
                    text: "Visitors"
                },
                exportOptions: {
                    image: false,
                    print: false
                },
                dataSeries: [{
                    seriesType: "area",
                    collectionAlias: "Q Data",
                    data: performance
                }]
            });

            $("#shieldui-chart2").shieldChart({
                theme: "dark",
                primaryHeader: {
                    text: "Traffic Per week"
                },
                exportOptions: {
                    image: false,
                    print: false
                },
                dataSeries: [{
                    seriesType: "pie",
                    collectionAlias: "traffic",
                    data: visits
                }]
            });

            $("#shieldui-grid1").shieldGrid({
                dataSource: {
                    data: traffic
                },
                sorting: {
                    multiple: true
                },
                rowHover: false,
                paging: false,
                columns: [
                { field: "Source", width: "170px", title: "Source" },
                { field: "Amount", title: "Amount" },
                { field: "Change", title: "Change", format: formatFunction },
                { field: "Percent", title: "Percent.,%", format: "{0} %" },
                { field: "Target", title: "Target" },
                ]
            });

            $("#shieldui-grid1").swidget().sort("Change", true);
        });

        function formatFunction(item) {
            if (item.Change > 40) {
                return "<span style='color: green;'> <strong>" + item.Change + "</strong></span>";
            }
            else {
                return "<span style='color: red;'>" + item.Change + "</span>";
            }
        }
    </script>

</body>
</html>