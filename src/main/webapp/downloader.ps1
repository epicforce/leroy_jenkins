$client = new-object System.Net.WebClient
$http = $args[1]
$path = $args[0]
$client.DownloadFile($http,$path)