
process.stdin.on("data",
  (chunk) => {
    var msg = chunk.toString("utf8");
    if (msg == "exit"){ // for after calling process.disconnect()
      process.exit(42);
    } else {
      process.stdout.write(msg);
    }
  });

process.on("message",
  (msg) => { // ["stderr" ["data" ["some string"]]]
    var key = msg[0];
    var val = msg[1];

    switch (key){
      case "stderr": // only data events
        process.stderr.write(val[1][0]);
        break;
      case "stdout": // only data events
        process.stdout.write(val[1][0]);
        break;
      case "message":
        process.send(val[0], val[1]); //obj + handle
        break;
      case "disconnect":
        process.disconnect();
        break;
    }
  });

process.on("uncaughtException",
  (err) => {
    process.send(["uncaughtException", {stack: err.stack, msg: err.message}]);
    process.exit(1);
  });

//fail safe
setTimeout(()=>{process.exit(1)},5000).unref();
