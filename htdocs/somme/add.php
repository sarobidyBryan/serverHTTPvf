<html>

<head>
    <title>Calcul Somme</title>

   
</head>

<body>
    <section class="vh-100">
        <div class="container py-5">
            <h1>Somme de 2 nombres</h1>
            <div class="row">
                <div class="col-xl-4 col-md-6 col-lg-4 col-sm-8 col-8 m-5">
                    <?php
                    // print_r($_GET);                    
                    if (isset($_POST['n1']) && isset($_POST['n2'])) {
                        $n1 = $_POST['n1'];
                        $n2 = $_POST['n2'];
                        $sum = $n1 + $n2;
                        echo "<div>Number 1 = $n1</div>
                <div>Number 2 = $n2</div>
                <h3>Sum = $sum</h3>";
                    } 
                    ?>
                </div>
            </div>
        </div>
    </section>

</body>

</html>