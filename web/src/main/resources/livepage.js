// Unique identifier to tie to LiveConnection
var liveId = '%1$s';
// Last message id received
var liveMessageId = %2$s;
// Set to true if currently in the process of an AJAX call
var liveSending = false;
// Message queue
var liveQueue = [];
// Currently sending data
var liveData = null;
// Number of times the AJAX send has failed
var failures = 0;
// Maximum number of times the send can fail before it gives up
var maxFailures = %3$s;
// Debug mode defines whether additional information is output to the console logging.
var debugMode = %5$s;
// Used to not fire stylechange events if changes are coming from the server
var applyingChanges = false;

function liveAdd(parentId, index, tagContent) {
    var parent = $('#' + parentId);
    liveInsertAt(parent, index, content);
}

function liveRemove(id) {
    var element = $('#' + id);
    element.remove();
}

function liveRemoveByIndex(parentId, index) {
    var parent = $('#' + parentId);
    parent.contents().eq(index).remove();
}

// Enqueue a JSON message
function liveMessage(message, replaceQuery) {
    liveEnqueue(message, replaceQuery);
    liveSend();     // Try to send immediately
}

function liveEnqueue(message, replaceQuery) {
    var replaceIndex = indexInQueue(replaceQuery);
    if (replaceIndex == -1) {
        liveQueue.push(message);
    } else {
//        console.log('Replacing: ' + JSON.stringify(liveQueue[replaceIndex]) + ' with ' + JSON.stringify(message));
        liveQueue[replaceIndex] = message;
    }
}

// Fire an event to the server on a specified id
function liveEvent(id, data, event, replaceQuery) {
    var json = {
        'id': id,
        'type': 'event',
        'event': event
    };
    if (data != null) {
        for (var key in data) {
            json[key] = data[key];
        }
    }
    liveMessage(json, replaceQuery);
}

function indexInQueue(query) {
    if (query == null) {
        return -1;
    }
    for (var i = 0; i < liveQueue.length; i++) {
        var json = liveQueue[i];
        if (jsonMatch(json, query)) {
            return i;
        }
    }
    return -1;
}

function jsonMatch(json, query) {
    for(var key in query) {
        if (json[key] != query[key]) {
            return false;
        }
    }
    return true;
}

// Add as a handler to fire live events to server
function liveEventHandler(e, data, fireChange, onlyLast) {
    var element = $(e.currentTarget);
    var id = element.attr('id');
    if (id == null) {           // Investigate this further
        element = $(e.target);
        id = element.attr('id');
    }
    if (id != null) {
        if (e.type == 'change' || fireChange) {
            var changeQuery = null;
            if (onlyLast) {
                changeQuery = {
                    'id': id,
                    'type': 'change'
                };
            }
            liveEnqueue({
                'id': id,
                'type': 'change',
                'value': element.val()
            }, changeQuery);
        }
        var lastQuery = null;
        if (onlyLast) {
            lastQuery = {
                'id': id,
                'type': 'event',
                'event': e.type
            };
        }
        if (e.type == 'keydown' || e.type == 'keypress' || e.type == 'keyup') {
            var json = {
                'id': id,
                'type': 'event',
                'event': e.type,
                'altKey': e.altKey,
                'char': e.char ? e.char : e.charCode,
                'ctrlKey': e.ctrlKey,
                'key': e.key ? e.key : e.keyCode,
                'locale': e.locale,
                'location': e.location,
                'metaKey': e.metaKey,
                'repeat': e.repeat,
                'shiftKey': e.shiftKey
            };
            liveMessage(json, lastQuery);
        } else {
            liveEvent(id, data, e.type, lastQuery);
        }
    } else {
        var target = $(e.target);
        console.log('liveEventHandler - No id found for ' + element + ', type: ' + e.type + ', targetId: ' + target.attr('id'));
    }
}

// Sends enqueued messages to the server
function liveSend() {
    if (!liveSending) {
        liveSending = true;
        var message = {
            'liveId': liveId,
            'liveMessageId': liveMessageId,
            'messages': liveQueue
        };
        liveData = liveQueue;       // Hold onto the data just in case we have to retry
        liveQueue = [];     // Reset live queue
        var url = window.location.href;
        $.ajax({
            type: 'POST',
            url: url,
            dataType: 'text',
            async: true,
            data: JSON.stringify(message),
            processData: false,
            cache: false,
            success: liveSendSuccessful,
            error: liveSendFailure,
            complete: liveSendComplete
        });
    }
}

function liveSendSuccessful(data) {
//    console.log('live send successful!');
    failures = 0;       // Reset failure counter
//    if (data != null) {
        applyingChanges = true;
        try {
            parseNextResponse(data);
        } finally {
            applyingChanges = false;
        }
//    } else {
//        console.log('No response data');
//        clearInterval(liveTimer);
//    }
}

function parseNextResponse(data) {
    if (data != null && data.length > 0) {
        if (data.charAt(0) == '[') {
            var p1 = data.indexOf('|');
            var p2 = data.indexOf('|', p1 + 1);
            var closeBracket = data.indexOf(']', p2);
            var id = parseInt(data.substring(1, p1));
            var instruction = data.substring(p1 + 1, p2);
            var contentLength = parseInt(data.substring(p2 + 1, closeBracket));
            var content = null;
            if (contentLength > -1) {
                content = data.substring(closeBracket + 1, closeBracket + 1 + contentLength);
            }
//            console.log('Parsed id: ' + id + ', instruction: ' + instruction + ', contentLength: ' + contentLength);
            if (liveMessageId < id) {       // Only evaluate once
                eval(instruction);
                liveMessageId = Math.max(id, liveMessageId);
            }
            var updated = data.substring(closeBracket + Math.max(contentLength, 0) + 2);
            parseNextResponse(updated);
        } else {
            console.log('Parsing error attempting to parse:' + data + ':')
        }
    }
}

function liveSendFailure(jqXHR, textStatus, errorThrown) {
    console.log('live send failure! status: ' + textStatus + ', error: ' + errorThrown);
    failures++;
    if (failures < maxFailures) {
        liveQueue = liveData.concat(liveQueue);     // Join the attempted data back to retry
    } else {
        console.log('live send failed ' + failures + ' times - stopping updates');
        clearInterval(liveTimer);
    }
}

function liveSendComplete() {
    liveSending = false;
    if (liveQueue.length > 0 && failures == 0) {
        liveSend();     // There's more in the queue to send
    }
}

function liveInsertAt(parent, index, element) {
    var lastIndex = parent.children().size();
    if (index < 0) {
        index = Math.max(0, lastIndex + 1, index);
    }
    parent.append(element);
    if (index < lastIndex) {
        parent.children().eq(index).before(parent.children().last());
    }
}

JSON.stringify = JSON.stringify || function (obj) {
    var t = typeof (obj);
    if (t != "object" || obj === null) {
        // simple data type
        if (t == "string") obj = '"'+obj+'"';
        return String(obj);
    }
    else {
        // recurse array or object
        var n, v, json = [], arr = (obj && obj.constructor == Array);
        for (n in obj) {
            v = obj[n]; t = typeof(v);
            if (t == "string") v = '"'+v+'"';
            else if (t == "object" && v !== null) v = JSON.stringify(v);
            json.push((arr ? "" : '"' + n + '":') + String(v));
        }
        return (arr ? "[" : "{") + String(json) + (arr ? "]" : "}");
    }
};

// TODO: move this elsewhere for general usage
// TODO: test for performance impact
(function() {
    var orig = $.fn.css;
    $.fn.css = function() {
        orig.apply(this, arguments);
        if (!applyingChanges) {
            this.trigger('stylechange', {
                'propertyName': arguments[0],
                'propertyValue': arguments[1]
            });
        }
    }
})();

var liveTimer = null;

$(document).ready(function () {
    liveSend(); // Send immediately just in case we missed something
    liveTimer = setInterval(liveSend, %4$s);
});