/*
 * Copyright 2012 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Definitions for node's stream module. Depends on the events module.
 * @see http://nodejs.org/api/stream.html
 * @see https://github.com/joyent/node/blob/master/lib/stream.js
 * @externs
 * @author Daniel Wirtz <dcode@dcode.io>
 */

/**
 BEGIN_NODE_INCLUDE
 var stream = require('stream');
 END_NODE_INCLUDE
 */

var stream = {};



/**
 * @constructor
 * @param {Object=} options
 * @extends events.EventEmitter
 */
stream.Stream = function(options) {};


/**
 * @param {stream.Readable} src
 * @param {stream.Writable} dst
 * @param {function(*=)} cb
 * @return {Stream}
 */
stream.pipeline = function(src, dst, cb){}

/**
 * @param {stream.Writable} dest
 * @param {{end: boolean}=} pipeOpts
 * @return {stream.Writable}
 */
stream.Stream.prototype.pipe = function(dest, pipeOpts) {};

/**
 * @constructor
 * @extends stream.Readable
 */
stream.ReadableStream = function() {};

/**
 * @type {boolean}
 */
stream.ReadableStream.prototype.readable;

/**
 * @param {string=} encoding
 */
stream.ReadableStream.prototype.setEncoding = function(encoding) {};

/**
 */
stream.ReadableStream.prototype.destroy = function() {};

/**
 * @constructor
 * @extends stream.Writable
 */
stream.WritableStream = function() {};

/**
 */
stream.WritableStream.prototype.drain = function() {};

/**
 * @type {boolean}
 */
stream.WritableStream.prototype.writable;

/**
 * @param {string|buffer.Buffer} buffer
 * @param {string=} encoding
 */
stream.WritableStream.prototype.write = function(buffer, encoding) {};

/**
 * @param {string|buffer.Buffer=} buffer
 * @param {string=} encoding
 * @param {function(*=)=} cb
 */
stream.WritableStream.prototype.end = function(buffer, encoding, cb) {};

/**
 */
stream.WritableStream.prototype.destroy = function() {};

/**
 */
stream.WritableStream.prototype.destroySoon = function() {};

// Undocumented

/**
 * @constructor
 * @param {Object=} options
 * @extends stream.Stream
 */
stream.Readable = function(options) {};

/**
 * @type {boolean}
 * @deprecated
 */
stream.Readable.prototype.readable;

/**
 * @protected
 * @param {string|buffer.Buffer|null} chunk
 * @return {boolean}
 */
stream.Readable.prototype.push = function(chunk) {};

/**
 * @param {string|buffer.Buffer|null} chunk
 * @return {boolean}
 */
stream.Readable.prototype.unshift = function(chunk) {};

/**
 * @param {string} enc
 */
stream.Readable.prototype.setEncoding = function(enc) {};

/**
 * @param {number=} n
 * @return {buffer.Buffer|string|null}
 */
stream.Readable.prototype.read = function(n) {};

/**
 * @protected
 * @param {number} n
 */
stream.Readable.prototype._read = function(n) {};

/**
 * @param {stream.Writable=} dest
 * @return {stream.Readable}
 */
stream.Readable.prototype.unpipe = function(dest) {};

/**
 */
stream.Readable.prototype.resume = function() {};

/**
 */
stream.Readable.prototype.pause = function() {};

/**
 * @param {stream.Stream} stream
 * @return {stream.Readable}
 */
stream.Readable.prototype.wrap = function(stream) {};

/**
 * @constructor
 * @param {Object=} options
 * @extends stream.Stream
 */
stream.Writable = function(options) {};

/**
 * @deprecated
 * @type {boolean}
 */
stream.Writable.prototype.writable;

/**
 * @param {string|buffer.Buffer} chunk
 * @param {string=} encoding
 * @param {function(*=)=} cb
 * @return {boolean}
 */
stream.Writable.prototype.write = function(chunk, encoding, cb) {};

/**
 * @protected
 * @param {string|buffer.Buffer} chunk
 * @param {string} encoding
 * @param {function(*=)} cb
 */
stream.Writable.prototype._write = function(chunk, encoding, cb) {};

/**
 * @param {string|buffer.Buffer=} chunk
 * @param {string=} encoding
 * @param {function(*=)=} cb
 */
stream.Writable.prototype.end = function(chunk, encoding, cb) {};

/**
 * @constructor
 * @param {Object=} options
 * @extends stream.Readable
 * Xextends stream.Writable
 */
stream.Duplex = function(options) {};

/**
 * @type {boolean}
 */
stream.Duplex.prototype.allowHalfOpen;


/**
 * @param {Object=} options
 * @constructor
 * @extends stream.Duplex
 */
stream.Transform = function(options) {};

/**
 * @protected
 * @param {string|buffer.Buffer} chunk
 * @param {string} encoding
 * @param {function(*=)} cb
 */
stream.Transform._transform = function(chunk, encoding, cb) {};

/**
 * @protected
 * @param {function(*=)} cb
 */
stream.Transform._flush = function(cb) {};

/**
 * @param {Object=} options
 * @constructor
 * @extends stream.Transform
 */
stream.PassThrough = function(options) {};
/*
 * Copyright 2012 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Definitions for node's buffer module.
 * @see http://nodejs.org/api/buffer.html
 * @see https://github.com/joyent/node/blob/master/lib/buffer.js
 * @externs
 * @author Daniel Wirtz <dcode@dcode.io>
 */

/**
 BEGIN_NODE_INCLUDE
 var buffer = require('buffer');
 END_NODE_INCLUDE
 */

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
 * @param {number} size
 * @param {(string|!Buffer|number)=} fill
 * @param {string=} encoding
 * @return {!Buffer}
 */
buffer.Buffer.alloc;

/**
 * @param {!Array=} array
 * @return {!Buffer}
 */
buffer.Buffer.from;


/**
 * @param {!Buffer} other
 * @return {boolean}
 */
buffer.Buffer.prototype.equals = function(other) {};


/**
 * @param {string} encoding
 * @return {boolean}
 */
buffer.Buffer.isEncoding = function(encoding) {};

/**
 * @param {*} obj
 * @return {boolean}
 * @nosideeffects
 */
buffer.Buffer.isBuffer = function(obj) {};


/**
 * @param {string} string
 * @param {string=} encoding
 * @return {number}
 * @nosideeffects
 */
buffer.Buffer.byteLength = function(string, encoding) {};

/**
 * @param {Array.<buffer.Buffer>} list
 * @param {number=} totalLength
 * @return {buffer.Buffer}
 * @nosideeffects
 */
buffer.Buffer.concat = function(list, totalLength) {};

/**
 * @param {number} offset
 * @return {*}
 */
buffer.Buffer.prototype.get = function(offset) {};

/**
 * @param {number} offset
 * @param {*} v
 */
buffer.Buffer.prototype.set = function(offset, v) {};

/**
 * @param {string} string
 * @param {number|string=} offset
 * @param {number|string=} length
 * @param {number|string=} encoding
 * @return {*}
 */
buffer.Buffer.prototype.write = function(string, offset, length, encoding) {};

/**
 * @return {Array}
 */
buffer.Buffer.prototype.toJSON = function() {};

/**
 * @type {number}
 */
buffer.Buffer.prototype.length;

/**
 * @param {buffer.Buffer} targetBuffer
 * @param {number=} targetStart
 * @param {number=} sourceStart
 * @param {number=} sourceEnd
 * @return {buffer.Buffer}
 */
buffer.Buffer.prototype.copy = function(targetBuffer, targetStart, sourceStart, sourceEnd){};

/**
 * @param {buffer.Buffer} targetBuffer
 * @param {number=} targetStart
 * @param {number=} sourceStart
 * @param {number=} sourceEnd
 * @return {buffer.Buffer}
 */
buffer.Buffer.prototype.copy = function(targetBuffer, targetStart, sourceStart, sourceEnd){};

/**
 * @param {number=} start
 * @param {number=} end
 * @return {buffer.Buffer}
 * @nosideeffects
 */
buffer.Buffer.prototype.slice = function(start, end) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readUInt8 = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readUInt16LE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readUInt16BE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readUInt32LE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readUInt32BE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readInt8 = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readInt16LE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readInt16BE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readInt32LE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readInt32BE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readFloatLE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readFloatBE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readDoubleLE = function(offset, noAssert) {};

/**
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.readDoubleBE = function(offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeUInt8 = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeUInt16LE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeUInt16BE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeUInt32LE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeUInt32BE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeInt8 = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeInt16LE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeInt16BE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeInt32LE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeInt32BE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeFloatLE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeFloatBE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeDoubleLE = function(value, offset, noAssert) {};

/**
 * @param {number} value
 * @param {number} offset
 * @param {boolean=} noAssert
 * @return {number}
 */
buffer.Buffer.prototype.writeDoubleBE = function(value, offset, noAssert) {};

/**
 * @param {*} value
 * @param {number=} offset
 * @param {number=} end
 */
buffer.Buffer.prototype.fill = function(value, offset, end) {};

/**
 * @param {string=} encoding
 * @param {number=} start
 * @param {number=} end
 * @nosideeffects
 */
buffer.Buffer.prototype.toString = function(encoding, start, end) {};

/**
 * @type {number}
 */
buffer.Buffer.INSPECT_MAX_BYTES = 50;

/**
 * @param {number} size
 */
buffer.SlowBuffer = function(size) {};

/**
 *
 * @param {string} string
 * @param {number|string} offset
 * @param {number|string=} length
 * @param {number|string=} encoding
 * @return {*}
 */
buffer.SlowBuffer.prototype.write = function(string, offset, length, encoding) {};

/**
 * @param {number} start
 * @param {number} end
 * @return {buffer.Buffer}
 */
buffer.SlowBuffer.prototype.slice = function(start, end) {};

/**
 * @return {string}
 */
buffer.SlowBuffer.prototype.toString = function() {};

//
// Legacy
//

/**
 * @param {number=} start
 * @param {number=} end
 * @return {buffer.Buffer}
 */
buffer.Buffer.prototype.utf8Slice = function(start, end) {};

/**
 * @param {number=} start
 * @param {number=} end
 * @return {buffer.Buffer}
 */
buffer.Buffer.prototype.binarySlice = function(start, end) {};

/**
 * @param {number=} start
 * @param {number=} end
 * @return {buffer.Buffer}
 */
buffer.Buffer.prototype.asciiSlice = function(start, end) {};

/**
 * @param {string} string
 * @param {number=} offset
 * @return {buffer.Buffer}
 */
buffer.Buffer.prototype.utf8Write = function(string, offset) {};

/**
 * @param {string} string
 * @param {number=} offset
 * @return {buffer.Buffer}
 */
buffer.Buffer.prototype.binaryWrite = function(string, offset) {};

/**
 * @param {string} string
 * @param {number=} offset
 * @return {buffer.Buffer}
 */
buffer.Buffer.prototype.asciiWrite = function(string, offset) {};
/*
 * Copyright 2012 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Definitions for node's net module. Depends on the events and buffer modules.
 * @see http://nodejs.org/api/net.html
 * @see https://github.com/joyent/node/blob/master/lib/net.js
 * @externs
 * @author Daniel Wirtz <dcode@dcode.io>
 */

/**
 BEGIN_NODE_INCLUDE
 var net = require('net');
 END_NODE_INCLUDE
 */

/**
 * @type {Object.<string,*>}
 */
var net = {};

/**
 * @typedef {{allowHalfOpen: ?boolean}}
 */
net.CreateOptions;

/**
 * @param {(net.CreateOptions|function(...))=} options
 * @param {function(...)=} connectionListener
 * @return {net.Server}
 */
net.createServer = function(options, connectionListener) {};

/**
 * @typedef {{port: ?number, host: ?string, localAddress: ?string, path: ?string, allowHalfOpen: ?boolean}}
 */
net.ConnectOptions;

/**
 * @param {net.ConnectOptions|number|string} arg1
 * @param {(function(...)|string)=} arg2
 * @param {function(...)=} arg3
 */
net.connect = function(arg1, arg2, arg3) {};

/**
 * @param {net.ConnectOptions|number|string} arg1
 * @param {(function(...)|string)=} arg2
 * @param {function(...)=} arg3
 */
net.createConnection = function(arg1, arg2, arg3) {};

/**
 * @constructor
 * @extends events.EventEmitter
 */
net.Server = function() {};

/**
 *
 * @param {number|*} port
 * @param {(string|number|function(...))=} host
 * @param {(number|function(...))=} backlog
 * @param {function(...)=} callback
 */
net.Server.prototype.listen = function(port, host, backlog, callback) {};

/**
 * @param {function(...)=} callback
 */
net.Server.prototype.close = function(callback) {};

/**
 * @return {{port: number, family: string, address: string}}
 */
net.Server.prototype.address = function() {};

/**
 * @type {number}
 */
net.Server.prototype.maxConnectinos;

/**
 * @type {number}
 */
net.Server.prototype.connections;

/**
 * @constructor
 * @param {{fd: ?*, type: ?string, allowHalfOpen: ?boolean}=} options
 * @extends events.EventEmitter
 */
net.Socket = function(options) {};

/**
 * @param {number|string|function(...)} port
 * @param {(string|function(...))=} host
 * @param {function(...)=} connectListener
 */
net.Socket.prototype.connect = function(port, host, connectListener) {};

/**
 * @type {number}
 */
net.Socket.prototype.bufferSize;

/**
 * @param {?string=} encoding
 */
net.Socket.prototype.setEncoding = function(encoding) {};

/**
 * @param {string|buffer.Buffer} data
 * @param {(string|function(...))=}encoding
 * @param {function(...)=} callback
 */
net.Socket.prototype.write = function(data, encoding, callback) {};

/**
 * @param {(string|buffer.Buffer)=}data
 * @param {string=} encoding
 */
net.Socket.prototype.end = function(data, encoding) {};

/**
 */
net.Socket.prototype.destroy = function() {};

/**
 */
net.Socket.prototype.pause = function() {};

/**
 */
net.Socket.prototype.resume = function() {};

/**
 * @param {number} timeout
 * @param {function(...)=} callback
 */
net.Socket.prototype.setTimeout = function(timeout, callback) {};

/**
 * @param {boolean=} noDelay
 */
net.Socket.prototype.setNoDelay = function(noDelay) {};

/**
 * @param {(boolean|number)=} enable
 * @param {number=} initialDelay
 */
net.Socket.prototype.setKeepAlive = function(enable, initialDelay) {};

/**
 * @return {string}
 */
net.Socket.prototype.address = function() {};

/**
 * @type {?string}
 */
net.Socket.prototype.remoteAddress;

/**
 * @type {?number}
 */
net.Socket.prototype.remotePort;

/**
 * @type {number}
 */
net.Socket.prototype.bytesRead;

/**
 * @type {number}
 */
net.Socket.prototype.bytesWritten;

/**
 * @param {*} input
 * @return {number}
 */
net.isIP = function(input) {};

/**
 * @param {*} input
 * @return {boolean}
 */
net.isIPv4 = function(input) {};

/**
 * @param {*} input
 * @return {boolean}
 */
net.isIPv6 = function(input) {};
/*
 * Copyright 2012 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Definitions for node's path module.
 * @see http://nodejs.org/api/path.html
 * @externs
 * @author Daniel Wirtz <dcode@dcode.io>
 */

/**
 BEGIN_NODE_INCLUDE
 var path = require('path');
 END_NODE_INCLUDE
 */

/**
 * @type {Object.<string,*>}
 */
var path = {};

/**
 * @param {string} p
 * @return {boolean}
 * @nosideeffects
 */
path.isAbsolute = function(p) {};

/**
 * @param {string} p
 * @return {string}
 * @nosideeffects
 */
path.normalize = function(p) {};

/**
 * @param {...string} var_args
 * @return {string}
 * @nosideeffects
 */
path.join = function(var_args) {};

/**
 * @param {string} from
 * @param {string=} to
 * @return {string}
 * @nosideeffects
 */
path.resolve = function(from, to) {};

/**
 * @param {string} from
 * @param {string} to
 * @return {string}
 * @nosideeffects
 */
path.relative = function(from, to) {};

/**
 * @param {string} p
 * @return {string}
 * @nosideeffects
 */
path.dirname = function(p) {};

/**
 * @param {string} p
 * @param {string=} ext
 * @return {string}
 * @nosideeffects
 */
path.basename = function(p, ext) {};

/**
 * @param {string} p
 * @return {string}
 * @nosideeffects
 */
path.extname = function(p) {};

/**
 * @type {string}
 */
path.sep;
/*
 * Copyright 2012 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Definitions for node's "events" module.
 * @see http://nodejs.org/api/events.html
 * @see https://github.com/joyent/node/blob/master/lib/events.js
 * @externs
 * @author Daniel Wirtz <dcode@dcode.io>
 */

/**
BEGIN_NODE_INCLUDE
var events = require('events');
END_NODE_INCLUDE
 */

/**
 * @type {Object.<string,*>}
 */
var events = {};

/**
 * @constructor
 */
events.EventEmitter = function() {};

/**
 * @param {string} event
 * @param {function(...)} listener
 * @return {events.EventEmitter}
 */
events.EventEmitter.prototype.addListener = function(event, listener) {};

/**
 * @param {string} event
 * @param {function(...)} listener
 * @return {events.EventEmitter}
 */
events.EventEmitter.prototype.on = function(event, listener) {};

/**
 * @param {string} event
 * @param {function(...)} listener
 * @return {events.EventEmitter}
 */
events.EventEmitter.prototype.once = function(event, listener) {};

/**
 * @param {string} event
 * @param {function(...)} listener
 * @return {events.EventEmitter}
 */
events.EventEmitter.prototype.removeListener = function(event, listener) {};

/**
 * @param {string=} event
 * @return {events.EventEmitter}
 */
events.EventEmitter.prototype.removeAllListeners = function(event) {};

/**
 * @param {number} n
 */
events.EventEmitter.prototype.setMaxListeners = function(n) {};

/**
 * @param {string} event
 * @return {Array.<function(...)>}
 */
events.EventEmitter.prototype.listeners = function(event) {};

/**
 * @param {string} event
 * @param {...*} var_args
 * @return {boolean}
 */
events.EventEmitter.prototype.emit = function(event, var_args) {};

// Undocumented

/**
 * @type {boolean}
 */
events.usingDomains;

/**
 * @param {events.EventEmitter} emitter
 * @param {string} type
 */
events.EventEmitter.listenerCount = function(emitter, type) {};
/*
 * Copyright 2012 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Definitions for node's global process object. Depends on the stream module.
 * @see http://nodejs.org/api/process.html
 * @externs
 * @author Daniel Wirtz <dcode@dcode.io>
 */

/**
 * @constructor
 * @extends events.EventEmitter
 */
var process = function() {};

/**
 * @type {stream.ReadableStream}
 */
process.stdin;

/**
 * @type {stream.WritableStream}
 */
process.stdout;

/**
 * @type {stream.WritableStream}
 */
process.stderr;

/**
 * @type {Array.<string>}
 */
process.argv;

/**
 * @type {string}
 */
process.execPath;

/**
 */
process.abort = function() {};

/**
 * @param {string} directory
 */
process.chdir = function(directory) {};

/**
 * @return {string}
 * @nosideeffects
 */
process.cwd = function() {};

/**
 * @type {Object.<string,string>}
 */
process.env;

/**
 * @param {number=} code
 */
process.exit = function(code) {};

/**
 * @return {number}
 * @nosideeffects
 */
process.getgid = function() {};

/**
 * @param {number} id
 */
process.setgid = function(id) {};

/**
 * @return {number}
 * @nosideeffects
 */
process.getuid = function() {};

/**
 * @param {number} id
 */
process.setuid = function(id) {};

/**
 * @type {!string}
 */
process.version;

/**
 * @type {Object.<string,string>}
 */
process.versions;

/**
 * @type {Object.<string,*>}
 */
process.config;

/**
 * @param {number} pid
 * @param {string=} signal
 */
process.kill = function(pid, signal) {};

/**
 * @type {number}
 */
process.pid;

/**
 * @type {string}
 */
process.title;

/**
 * @type {string}
 */
process.arch;

/**
 * @type {string}
 */
process.platform;

/**
 * @return {Object.<string,number>}
 * @nosideeffects
 */
process.memoryUsage = function() {};

/**
 * @param {!function()} callback
 */
process.nextTick = function(callback) {};

/**
 * @param {number=} mask
 */
process.umask = function(mask) {};

/**
 * @return {number}
 * @nosideeffects
 */
process.uptime = function() {};

/**
 * @return {number}
 * @nosideeffects
 */
process.hrtime = function() {};
/*
 * Copyright 2012 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Definitions for node's fs module. Depends on the stream and events module.
 * @see http://nodejs.org/api/fs.html
 * @see https://github.com/joyent/node/blob/master/lib/fs.js
 * @externs
 * @author Daniel Wirtz <dcode@dcode.io>
 */

/**
 BEGIN_NODE_INCLUDE
 var fs = require('fs');
 END_NODE_INCLUDE
 */

var fs = {};

/**
 * @param {string} oldPath
 * @param {string} newPath
 * @param {function(...)=} callback
 */
fs.rename = function(oldPath, newPath, callback) {};

/**
 * @param {string} oldPath
 * @param {string} newPath
 */
fs.renameSync = function(oldPath, newPath) {};

/**
 * @param {*} fd
 * @param {number} len
 * @param {function(...)=} callback
 */
fs.truncate = function(fd, len, callback) {};

/**
 * @param {*} fd
 * @param {number} len
 */
fs.truncateSync = function(fd, len) {};

/**
 * @param {string} path
 * @param {number=} mode
 * @param {function(...)=} callback
 */
fs.access = function(path, mode, callback) {};

/**
 * @param {string} path
 * @param {number=} mode
 */
fs.accessSync = function(path, mode) {};

/**
 * @param {string} path
 * @param {number} uid
 * @param {number} gid
 * @param {function(...)=} callback
 */
fs.chown = function(path, uid, gid, callback) {};

/**
 * @param {string} path
 * @param {number} uid
 * @param {number} gid
 */
fs.chownSync = function(path, uid, gid) {};

/**
 * @param {*} fd
 * @param {number} uid
 * @param {number} gid
 * @param {function(...)=} callback
 */
fs.fchown = function(fd, uid, gid, callback) {};

/**
 * @param {*} fd
 * @param {number} uid
 * @param {number} gid
 */
fs.fchownSync = function(fd, uid, gid) {};

/**
 * @param {string} path
 * @param {number} uid
 * @param {number} gid
 * @param {function(...)=} callback
 */
fs.lchown = function(path, uid, gid, callback) {};

/**
 * @param {string} path
 * @param {number} uid
 * @param {number} gid
 */
fs.lchownSync = function(path, uid, gid) {};

/**
 * @param {string} path
 * @param {number} mode
 * @param {function(...)=} callback
 */
fs.chmod = function(path, mode, callback) {};

/**
 * @param {string} path
 * @param {number} mode
 */
fs.chmodSync = function(path, mode) {};

/**
 * @param {*} fd
 * @param {number} mode
 * @param {function(...)=} callback
 */
fs.fchmod = function(fd, mode, callback) {};

/**
 * @param {*} fd
 * @param {number} mode
 */
fs.fchmodSync = function(fd, mode) {};

/**
 * @param {string} path
 * @param {number} mode
 * @param {function(...)=} callback
 */
fs.lchmod = function(path, mode, callback) {};

/**
 * @param {string} path
 * @param {number} mode
 */
fs.lchmodSync = function(path, mode) {};

/**
 * @param {string} path
 * @param {function(string, fs.Stats)=} callback
 */
fs.stat = function(path, callback) {};

/**
 * @param {string} path
 * @return {fs.Stats}
 * @nosideeffects
 */
fs.statSync = function(path) {}

/**
 * @param {*} fd
 * @param {function(string, fs.Stats)=} callback
 */
fs.fstat = function(fd, callback) {};

/**
 * @param {*} fd
 * @return {fs.Stats}
 * @nosideeffects
 */
fs.fstatSync = function(fd) {}

/**
 * @param {string} path
 * @param {function(string, fs.Stats)=} callback
 */
fs.lstat = function(path, callback) {};

/**
 * @param {string} path
 * @return {fs.Stats}
 * @nosideeffects
 */
fs.lstatSync = function(path) {}

/**
 * @param {string} srcpath
 * @param {string} dstpath
 * @param {function(...)=} callback
 */
fs.link = function(srcpath, dstpath, callback) {};

/**
 * @param {string} srcpath
 * @param {string} dstpath
 */
fs.linkSync = function(srcpath, dstpath) {};

/**
 * @param {string} srcpath
 * @param {string} dstpath
 * @param {string=} type
 * @param {function(...)=} callback
 */
fs.symlink = function(srcpath, dstpath, type, callback) {};

/**
 * @param {string} srcpath
 * @param {string} dstpath
 * @param {string=} type
 */
fs.symlinkSync = function(srcpath, dstpath, type) {};

/**
 * @param {string} path
 * @param {function(string, string)=} callback
 */
fs.readlink = function(path, callback) {};

/**
 * @param {string} path
 * @return {string}
 * @nosideeffects
 */
fs.readlinkSync = function(path) {};

/**
 * @param {string} path
 * @param {Object.<string,string>|function(string, string)=} cache
 * @param {function(string, string)=} callback
 */
fs.realpath = function(path, cache, callback) {};

/**
 * @param {string} path
 * @param {Object.<string,string>=} cache
 * @return {string}
 * @nosideeffects
 */
fs.realpathSync = function(path, cache) {};

/**
 * @param {string} path
 * @param {function(...)=} callback
 */
fs.unlink = function(path, callback) {};

/**
 * @param {string} path
 */
fs.unlinkSync = function(path) {};

/**
 * @param {string} path
 * @param {function(...)=} callback
 */
fs.rmdir = function(path, callback) {};

/**
 * @param {string} path
 */
fs.rmdirSync = function(path) {};

/**
 * @param {string} path
 * @param {number=} mode
 * @param {function(...)=} callback
 */
fs.mkdir = function(path, mode, callback) {};

/**
 * @param {string} path
 * @param {number=} mode
 */
fs.mkdirSync = function(path, mode) {};

/**
 * @param {string} path
 * @param {function(string,Array.<string>)=} callback
 */
fs.readdir = function(path, callback) {};

/**
 * @param {string} path
 * @return {Array.<string>}
 * @nosideeffects
 */
fs.readdirSync = function(path) {};

/**
 * @param {*} fd
 * @param {function(...)=} callback
 */
fs.close = function(fd, callback) {};

/**
 * @param {*} fd
 */
fs.closeSync = function(fd) {};

/**
 * @param {string} path
 * @param {string} flags
 * @param {number=} mode
 * @param {function(string, *)=} callback
 */
fs.open = function(path, flags, mode, callback) {};

/**
 * @param {string} path
 * @param {string} flags
 * @param {number=} mode
 * @return {*}
 * @nosideeffects
 */
fs.openSync = function(path, flags, mode) {};

/**
 * @param {string} path
 * @param {number|Date} atime
 * @param {number|Date} mtime
 * @param {function(...)=} callback
 */
fs.utimes = function(path, atime, mtime, callback) {};

/**
 * @param {string} path
 * @param {number|Date} atime
 * @param {number|Date} mtime
 * @nosideeffects
 */
fs.utimesSync = function(path, atime, mtime) {};

/**
 * @param {*} fd
 * @param {number|Date} atime
 * @param {number|Date} mtime
 * @param {function(...)=} callback
 */
fs.futimes = function(fd, atime, mtime, callback) {};

/**
 * @param {*} fd
 * @param {number|Date} atime
 * @param {number|Date} mtime
 * @nosideeffects
 */
fs.futimesSync = function(fd, atime, mtime) {};

/**
 * @param {*} fd
 * @param {function(...)=} callback
 */
fs.fsync = function(fd, callback) {};

/**
 * @param {*} fd
 */
fs.fsyncSync = function(fd) {};

/**
 * @param {*} fd
 * @param {*} buffer
 * @param {number} offset
 * @param {number} length
 * @param {number} position
 * @param {function(string, number, *)=} callback
 */
fs.write = function(fd, buffer, offset, length, position, callback) {};

/**
 * @param {*} fd
 * @param {*} buffer
 * @param {number} offset
 * @param {number} length
 * @param {number} position
 * @return {number}
 */
fs.writeSync = function(fd, buffer, offset, length, position) {};

/**
 * @param {*} fd
 * @param {*} buffer
 * @param {number} offset
 * @param {number} length
 * @param {number} position
 * @param {function(string, number, *)=} callback
 */
fs.read = function(fd, buffer, offset, length, position, callback) {};

/**
 * @param {*} fd
 * @param {*} buffer
 * @param {number} offset
 * @param {number} length
 * @param {number} position
 * @return {number}
 * @nosideeffects
 */
fs.readSync = function(fd, buffer, offset, length, position) {};

/**
 * @param {string} filename
 * @param {string|{encoding:(string|undefined),flag:(string|undefined)}|function(string, (string|buffer.Buffer))=} encodingOrOptions
 * @param {function(string, (string|buffer.Buffer))=} callback
 */
fs.readFile = function(filename, encodingOrOptions, callback) {};

/**
 * @param {string} filename
 * @param {string|{encoding:(string|undefined),flag:(string|undefined)}=} encodingOrOptions
 * @return {string|buffer.Buffer}
 * @nosideeffects
 */
fs.readFileSync = function(filename, encodingOrOptions) {};

/**
 * @param {string} filename
 * @param {*} data
 * @param {string|{encoding:(string|undefined),mode:(number|undefined),flag:(string|undefined)}|function(string)=} encodingOrOptions
 * @param {function(string)=} callback
 */
fs.writeFile = function(filename, data, encodingOrOptions, callback) {};

/**
 * @param {string} filename
 * @param {*} data
 * @param {string|{encoding:(string|undefined),mode:(number|undefined),flag:(string|undefined)}|function(string)=} encodingOrOptions
 */
fs.writeFileSync = function(filename, data, encodingOrOptions) {};

/**
 * @param {string} src
 * @param {string} dest
 * @param {number|function(...)=} flags
 * @param {function(...)=} callback
 */
fs.copyFile = function(src, dest, flags, callback) {};

/**
 * @param {string} src
 * @param {string} dest
 * @param {number=} flags
 */
fs.copyFileSync = function(src, dest, flags) {};

/**
 * @param {string} filename
 * @param {*} data
 * @param {string|function(string)=} encoding
 * @param {function(string)=} callback
 */
fs.appendFile = function(filename, data, encoding, callback) {};

/**
 * @param {string} filename
 * @param {*} data
 * @param {string|function(string)=} encoding
 */
fs.appendFileSync = function(filename, data, encoding) {};

/**
 * @param {string} filename
 * @param {{persistent: boolean, interval: number}|function(*,*)=} options
 * @param {function(*,*)=} listener
 */
fs.watchFile = function(filename, options, listener) {};

/**
 * @param {string} filename
 * @param {function(string, string)=} listener
 */
fs.unwatchFile = function(filename, listener) {};

/**
 *
 * @param {string} filename
 * @param {{persistent: boolean}|function(string, string)=} options
 * @param {function(string, string)=} listener
 * @return {fs.FSWatcher}
 */
fs.watch = function(filename, options, listener) {};

/**
 * @param {string} path
 * @param {function(boolean)} callback
 */
fs.exists = function(path, callback) {};

/**
 * @param {string} path
 * @nosideeffects
 */
fs.existsSync = function(path) {};

/**
 * @constructor
 */
fs.Stats = function() {};

/**
 * @return {boolean}
 * @nosideeffects
 */
fs.Stats.prototype.isFile = function() {};

/**
 * @return {boolean}
 * @nosideeffects
 */
fs.Stats.prototype.isDirectory = function() {};

/**
 * @return {boolean}
 * @nosideeffects
 */
fs.Stats.prototype.isBlockDevice = function() {};

/**
 * @return {boolean}
 * @nosideeffects
 */
fs.Stats.prototype.isCharacterDevice = function() {};

/**
 * @return {boolean}
 * @nosideeffects
 */
fs.Stats.prototype.isSymbolicLink = function() {};

/**
 * @return {boolean}
 * @nosideeffects
 */
fs.Stats.prototype.isFIFO = function() {};

/**
 * @return {boolean}
 * @nosideeffects
 */
fs.Stats.prototype.isSocket = function() {};

/**
 * @type {number}
 */
fs.Stats.prototype.dev = 0;

/**
 * @type {number}
 */
fs.Stats.prototype.ino = 0;

/**
 * @type {number}
 */
fs.Stats.prototype.mode = 0;

/**
 * @type {number}
 */
fs.Stats.prototype.nlink = 0;

/**
 * @type {number}
 */
fs.Stats.prototype.uid = 0;

/**
 * @type {number}
 */
fs.Stats.prototype.gid = 0;

/**
 * @type {number}
 */
fs.Stats.prototype.rdev = 0;

/**
 * @type {number}
 */
fs.Stats.prototype.size = 0;

/**
 * @type {number}
 */
fs.Stats.prototype.blkSize = 0;

/**
 * @type {number}
 */
fs.Stats.prototype.blocks = 0;

/**
 * @type {Date}
 */
fs.Stats.prototype.atime;

/**
 * @type {Date}
 */
fs.Stats.prototype.mtime;

/**
 * @type {Date}
 */
fs.Stats.prototype.ctime;

/**
 * @param {string} path
 * @param {{flags: string, encoding: ?string, fd: *, mode: number, bufferSize: number}=} options
 * @return {fs.ReadStream}
 * @nosideeffects
 */
fs.createReadStream = function(path, options) {};

/**
 * @constructor
 * @extends stream.ReadableStream
 */
fs.ReadStream = function() {};

/**
 * @param {string} path
 * @param {{flags: string, encoding: ?string, mode: number}=} options
 * @return {fs.WriteStream}
 * @nosideeffects
 */
fs.createWriteStream = function(path, options) {};

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
 */
fs.FSWatcher.prototype.close = function() {};
/*
 * Copyright 2012 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Definitions for node's os module.
 * @see http://nodejs.org/api/os.html
 * @externs
 * @author Daniel Wirtz <dcode@dcode.io>
 */

/**
 BEGIN_NODE_INCLUDE
 var os = require('os');
 END_NODE_INCLUDE
 */

var os = {};

/**
 * @return {string}
 * @nosideeffects
 */
os.tmpdir = function() {};
os.tmpDir = function() {};

/**
 * @return {string}
 * @nosideeffects
 */
os.hostname = function() {};

/**
 * @return {string}
 * @nosideeffects
 */
os.type = function() {};

/**
 * @return {string}
 * @nosideeffects
 */
os.platform = function() {};

/**
 * @return {string}
 * @nosideeffects
 */
os.arch = function() {};

/**
 * @return {string}
 * @nosideeffects
 */
os.release = function() {};

/**
 * @return {number}
 * @nosideeffects
 */
os.uptime = function() {};

/**
 * @return {Array.<number>}
 * @nosideeffects
 */
os.loadavg = function() {};

/**
 * @return {number}
 * @nosideeffects
 */
os.totalmem = function() {};

/**
 * @return {number}
 * @nosideeffects
 */
os.freemem = function() {};

/**
 * @typedef {{model: string, speed: number, times: {user: number, nice: number, sys: number, idle: number, irg: number}}}
 */
var osCpusInfo;

/**
 * @return {Array.<osCpusInfo>}
 * @nosideeffects
 */
os.cpus = function() {};

/**
 * @typedef {{address: string, family: string, internal: boolean}}
 */
var osNetworkInterfacesInfo;

/**
 * @return {Object.<string,osNetworkInterfacesInfo>}
 * @nosideeffects
 */
os.networkInterfaces = function() {};

/**
 * @type {string}
 */
os.EOL;
/*
 * Copyright 2012 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Definitions for node's child_process module. Depends on the events module.
 * @see http://nodejs.org/api/child_process.html
 * @see https://github.com/joyent/node/blob/master/lib/child_process.js
 * @externs
 * @author Daniel Wirtz <dcode@dcode.io>
 */

/**
 BEGIN_NODE_INCLUDE
 var child_process = require('child_process');
 END_NODE_INCLUDE
 */

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

/**
 * @type {stream.ReadableStream}
 */
child_process.ChildProcess.prototype.stdin;

/**
 * @type {stream.WritableStream}
 */
child_process.ChildProcess.prototype.stdout;

/**
 * @type {stream.WritableStream}
 */
child_process.ChildProcess.prototype.stderr;

/**
 * @type {number}
 */
child_process.ChildProcess.prototype.pid;

/**
 * @param {string=} signal
 */
child_process.ChildProcess.prototype.kill = function(signal) {};

/**
 * @param {Object.<string,*>} message
 * @param {*} sendHandle
 */
child_process.ChildProcess.prototype.send = function(message, sendHandle) {};

/**
 */
child_process.ChildProcess.prototype.disconnect = function() {};

/**
 * @typedef {{cwd: string, stdio: (Array|string), customFds: Array, env: Object.<string,*>, detached: boolean, uid: number, gid: number, encoding: string, timeout: number, maxBuffer: number, killSignal: string}}
 */
child_process.Options;

/**
 * @param {string} command
 * @param {Array.<string>=} args
 * @param {child_process.Options=} options
 * @return {child_process.ChildProcess}
 */
child_process.ChildProcess.spawn = function(command, args, options) {};

/**
 * @param {string} command
 * @param {child_process.Options|function(Error, buffer.Buffer, buffer.Buffer)=} options
 * @param {function(Error, buffer.Buffer, buffer.Buffer)=} callback
 * @return {child_process.ChildProcess}
 */
child_process.exec = function(command, options, callback) {};

/**
 * @param {string} file
 * @param {Array.<string>} args
 * @param {child_process.Options} options
 * @param {function(Error, buffer.Buffer, buffer.Buffer)} callback
 * @return {child_process.ChildProcess}
 */
child_process.execFile = function(file, args, options, callback) {};

/**
 * @param {string} modulePath
 * @param {Array.<string>=} args
 * @param {child_process.Options=} options
 * @return {child_process.ChildProcess}
 */
child_process.fork = function(modulePath, args, options) {};
