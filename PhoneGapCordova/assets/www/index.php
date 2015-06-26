<?php 
session_start();
include_once 'classes/init.php';
include_once 'classes/saveappt.php';
define('MAGIC_QUOTES_ACTIVE', get_magic_quotes_gpc()); 
 ?>
<!DOCTYPE html>
<html lang="en">
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
                    <li class="selected"><a href="index.php"><i class="fa fa-bullseye"></i> Update Status</a></li>
                    <li><a href="history.php"><i class="fa fa-table"></i> History</a></li>
                    <li><a href="settings.php"><i class="fa fa-cogs"></i> Settings</a></li>
                </ul>
            </div>
        </nav>

        <div id="page-wrapper">

            <div class="row">
                <div class="col-lg-12">
                     <p><?php echo $message1; ?> </p>
                          
                             </div>
                    <h1 align="center">Status Update </br><small>User ID: <?php   $eid = mysqli_query($con3, "SELECT employeeid FROM employee ORDER BY id DESC LIMIT 1");
														 while ($row = $eid->fetch_assoc()){
															echo $row['employeeid'];
														 } ?></small></h1>
<!--Dissmissable Confirmation-->
                    
                        
                      
                   
<!--Dissmissable Confirmation-->
                </div>
            </div>
<p align="center">Enter the 5 digit Appointment Code:
<form role="form" action="index.php" method="post" name="maint" id="maint" >
        <table align="center" width="150px"><tr><td>
        <input class="form-control" type="text" name="appt" id="appt" maxlength="5" pattern=".{5,}" required/></br>
   </td></tr></table>

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
    
<p align="center"><button type="submit" class="btn btn-danger btn-lg" name="checkin">Check In</button></p>
<p align="center"><button type="submit" class="btn btn-success btn-lg" name="checkout">Check Out</button></p>
<p align="center"><button type="submit" class="btn btn-info btn-lg" name="qccheck">Qualiy Check</button></p>
  
</form></p>
           <!-- <div class="row">

                <div class="col-md-8">
                    <div class="panel panel-primary">
                        <div class="panel-heading">
                            <h3 class="panel-title"><i class="fa fa-bar-chart-o"></i> Visits Based on a 10 days data</h3>
                        </div>
                        <div class="panel-body">
                            <div id="shieldui-chart1"></div>
                        </div>
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="panel panel-primary">
                        <div class="panel-heading">
                            <h3 class="panel-title"><i class="fa fa-rss"></i> Feed</h3>
                        </div>
                        <div class="panel-body feed">
                            <section class="feed-item">
                                <div class="icon pull-left">
                                    <i class="fa fa-comment"></i>
                                </div>
                                <div class="feed-item-body">
                                    <div class="text">
                                        <a href="#">John Doe</a> commented on <a href="#">What Makes Good Code Good</a>.
                                    </div>
                                    <div class="time pull-left">
                                        3 h
                                    </div>
                                </div>
                            </section>
                            <section class="feed-item">
                                <div class="icon pull-left">
                                    <i class="fa fa-check"></i>
                                </div>
                                <div class="feed-item-body">
                                    <div class="text">
                                        <a href="#">Merge request #42</a> has been approved by <a href="#">Jessica Lori</a>.
                                    </div>
                                    <div class="time pull-left">
                                        10 h
                                    </div>
                                </div>
                            </section>
                            <section class="feed-item">
                                <div class="icon pull-left">
                                    <i class="fa fa-plus-square-o"></i>
                                </div>
                                <div class="feed-item-body">
                                    <div class="text">
                                        New user <a href="#">Greg Wilson</a> registered.
                                    </div>
                                    <div class="time pull-left">
                                        Today
                                    </div>
                                </div>
                            </section>
                            <section class="feed-item">
                                <div class="icon pull-left">
                                    <i class="fa fa-bolt"></i>
                                </div>
                                <div class="feed-item-body">
                                    <div class="text">
                                        Server fail level raises above normal. <a href="#">See logs</a> for details.
                                    </div>
                                    <div class="time pull-left">
                                        Yesterday
                                    </div>
                                </div>
                            </section>
                            <section class="feed-item">
                                <div class="icon pull-left">
                                    <i class="fa fa-archive"></i>
                                </div>
                                <div class="feed-item-body">
                                    <div class="text">
                                        <a href="#">Database usage report</a> is ready.
                                    </div>
                                    <div class="time pull-left">
                                        Yesterday
                                    </div>
                                </div>
                            </section>
                            <section class="feed-item">
                                <div class="icon pull-left">
                                    <i class="fa fa-shopping-cart"></i>
                                </div>
                                <div class="feed-item-body">
                                    <div class="text">
                                        <a href="#">Order #233985</a> needs additional processing.
                                    </div>
                                    <div class="time pull-left">
                                        Wednesday
                                    </div>
                                </div>
                            </section>
                            <section class="feed-item">
                                <div class="icon pull-left">
                                    <i class="fa fa-arrow-down"></i>
                                </div>
                                <div class="feed-item-body">
                                    <div class="text">
                                        <a href="#">Load more...</a>
                                    </div>
                                </div>
                            </section>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-lg-12">
                    <div class="panel panel-primary">
                        <div class="panel-heading">
                            <h3 class="panel-title"><i class="fa fa-bar-chart-o"></i> Traffic Sources One month tracking </h3>
                        </div>
                        <div class="panel-body">
                            <div id="shieldui-grid1"></div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-lg-4">
                    <div class="panel panel-primary">
                        <div class="panel-heading">
                            <h3 class="panel-title"><i class="fa fa-bar-chart-o"></i> Logins per week</h3>
                        </div>
                        <div class="panel-body">
                            <div id="shieldui-chart2"></div>
                        </div>
                    </div>
                </div>
                <div class="col-lg-4">
                    <div class="panel panel-primary">
                        <div class="panel-heading">
                            <h3 class="panel-title"><i class="fa fa-magnet"></i> Server Overview</h3>
                        </div>
                        <div class="panel-body">
                            <ul class="server-stats">
                                <li>
                                    <div class="key pull-right">CPU</div>
                                    <div class="stat">
                                        <div class="info">60% / 37°C / 3.3 Ghz</div>
                                        <div class="progress progress-small">
                                            <div style="width: 70%;" class="progress-bar progress-bar-danger"></div>
                                        </div>
                                    </div>
                                </li>
                                <li>
                                    <div class="key pull-right">Mem</div>
                                    <div class="stat">
                                        <div class="info">29% / 4GB (16 GB)</div>
                                        <div class="progress progress-small">
                                            <div style="width: 29%;" class="progress-bar"></div>
                                        </div>
                                    </div>
                                </li>
                                <li>
                                    <div class="key pull-right">LAN</div>
                                    <div class="stat">
                                        <div class="info">6 Mb/s <i class="fa fa-caret-down"></i>&nbsp; 3 Mb/s <i class="fa fa-caret-up"></i></div>
                                        <div class="progress progress-small">
                                            <div style="width: 48%;" class="progress-bar progress-bar-inverse"></div>
                                        </div>
                                    </div>
                                </li>
                            </ul>
                        </div>
                    </div>

                </div>
                <div class="col-lg-4">

                    <header>
                        <ul class="nav nav-tabs">
                            <li class="active">
                                <a data-toggle="tab" href="#stats">Users</a>
                            </li>
                            <li class="">
                                <a data-toggle="tab" href="#report">Favorites</a>
                            </li>
                            <li class="">
                                <a data-toggle="tab" href="#dropdown1">Commenters</a>
                            </li>
                        </ul>
                    </header>
                    <div class="body tab-content">
                        <div class="tab-pane clearfix active" id="stats">
                            <h5 class="tab-header"><i class="fa fa-calendar-o fa-2x"></i> Last logged-in users</h5>
                            <ul class="news-list">
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Ivan Gorge</a></div>
                                        <div class="position">Software Engineer</div>
                                        <div class="time">Last logged-in: Mar 12, 11:11</div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Roman Novak</a></div>
                                        <div class="position">Product Designer</div>
                                        <div class="time">Last logged-in: Mar 12, 19:02</div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Teras Uotul</a></div>
                                        <div class="position">Chief Officer</div>
                                        <div class="time">Last logged-in: Jun 16, 2:34</div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Deral Ferad</a></div>
                                        <div class="position">Financial Assistant</div>
                                        <div class="time">Last logged-in: Jun 18, 4:20</div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Konrad Polerd</a></div>
                                        <div class="position">Sales Manager</div>
                                        <div class="time">Last logged-in: Jun 18, 5:13</div>
                                    </div>
                                </li>
                            </ul>
                        </div>
                        <div class="tab-pane" id="report">
                            <h5 class="tab-header"><i class="fa fa-star fa-2x"></i> Popular contacts</h5>
                            <ul class="news-list news-list-no-hover">
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Pol Johnsson</a></div>
                                        <div class="options">
                                            <button class="btn btn-xs btn-success">
                                                <i class="fa fa-phone"></i>
                                                Call
                                            </button>
                                            <button class="btn btn-xs btn-warning">
                                                <i class="fa fa-envelope-o"></i>
                                                Message
                                            </button>
                                        </div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Terry Garel</a></div>
                                        <div class="options">
                                            <button class="btn btn-xs btn-success">
                                                <i class="fa fa-phone"></i>
                                                Call
                                            </button>
                                            <button class="btn btn-xs btn-warning">
                                                <i class="fa fa-envelope-o"></i>
                                                Message
                                            </button>
                                        </div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Eruos Forkal</a></div>
                                        <div class="options">
                                            <button class="btn btn-xs btn-success">
                                                <i class="fa fa-phone"></i>
                                                Call
                                            </button>
                                            <button class="btn btn-xs btn-warning">
                                                <i class="fa fa-envelope-o"></i>
                                                Message
                                            </button>
                                        </div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Remus Reier</a></div>
                                        <div class="options">
                                            <button class="btn btn-xs btn-success">
                                                <i class="fa fa-phone"></i>
                                                Call
                                            </button>
                                            <button class="btn btn-xs btn-warning">
                                                <i class="fa fa-envelope-o"></i>
                                                Message
                                            </button>
                                        </div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Lover Lund</a></div>
                                        <div class="options">
                                            <button class="btn btn-xs btn-success">
                                                <i class="fa fa-phone"></i>
                                                Call
                                            </button>
                                            <button class="btn btn-xs btn-warning">
                                                <i class="fa fa-envelope-o"></i>
                                                Message
                                            </button>
                                        </div>
                                    </div>
                                </li>
                            </ul>
                        </div>
                        <div class="tab-pane" id="dropdown1">
                            <h5 class="tab-header"><i class="fa fa-comments fa-2x"></i> Top Commenters</h5>
                            <ul class="news-list">
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Edin Garey</a></div>
                                        <div class="comment">
                                            Nemo enim ipsam voluptatem quia voluptas sit aspernatur 
                                            aut odit aut fugit,sed quia
                                        </div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Firel Lund</a></div>
                                        <div class="comment">
                                            Duis aute irure dolor in reprehenderit in voluptate velit
                                             esse cillum dolore eu fugiat.
                                        </div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Jessica Desingter</a></div>
                                        <div class="comment">
                                            Excepteur sint occaecat cupidatat non proident, sunt in
                                             culpa qui officia deserunt.
                                        </div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Novel Forel</a></div>
                                        <div class="comment">
                                            Sed ut perspiciatis, unde omnis iste natus error sit voluptatem accusantium doloremque.
                                        </div>
                                    </div>
                                </li>
                                <li>
                                    <i class="fa fa-user fa-4x pull-left"></i>
                                    <div class="news-item-info">
                                        <div class="name"><a href="#">Wedol Reier</a></div>
                                        <div class="comment">
                                            Laudantium, totam rem aperiam eaque ipsa, quae ab illo inventore veritatis
                                            et quasi.
                                        </div>
                                    </div>
                                </li>
                            </ul>
                        </div>
                    </div>-->

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
