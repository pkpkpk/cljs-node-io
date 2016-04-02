/**
 * @type {Object.<string,*>}
 */
var buffer = {};

/**
 * @param {...*} var_args
 * @constructor
 * @nosideeffects
 */
buffer.Buffer = function(var_args) {};

/**
 * @type {Object.<string,*>}
 */
var events = {};

/**
 * @constructor
 */
events.EventEmitter = function() {};

var stream = {};

/**
 * @constructor
 * @param {Object=} options
 * @extends events.EventEmitter
 */
stream.Stream = function(options) {};

/**
 * @constructor
 * @extends stream.Readable
 */
stream.ReadableStream = function() {};

/**
 * @constructor
 * @extends stream.Writable
 */
stream.WritableStream = function() {};

/**
 * @constructor
 * @param {Object=} options
 * @extends stream.Stream
 */
stream.Readable = function(options) {};

/**
 * @constructor
 * @param {Object=} options
 * @extends stream.Stream
 */
stream.Writable = function(options) {};

/**
 * @constructor
 * @param {Object=} options
 * @extends stream.Readable
 * Xextends stream.Writable
 */
stream.Duplex = function(options) {};

/**
 * @param {Object=} options
 * @constructor
 * @extends stream.Duplex
 */
stream.Transform = function(options) {};

/**
 * @constructor
 * @extends stream.ReadableStream
 */
fs.ReadStream = function() {};

var fs = {};

/**
 * @constructor
 * @extends stream.WritableStream
 */
fs.WriteStream = function() {};

/**
 * @constructor
 * @extends events.EventEmitter
 */
fs.FSWatcher = function() {};

/**
 * @constructor
 */
fs.Stats = function() {};

/**
 * @type {Object.<string,*>}
 */
var child_process = {};

/**
 * @constructor
 * @param {...*} var_args
 * @extends events.EventEmitter
 */
child_process.ChildProcess = function(var_args) {}; // Private?
