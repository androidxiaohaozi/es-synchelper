var searchResult = [];
$("#startJob").on("click", function () {
    $("#message").html('');
    $("#startJob").prop('disabled', true);
    $.ajax({
        url: './launchjob',
        data: {
            whereSql: $('#whereSql').val(),
            force: $('#force').is(':checked')
        },
        method: 'POST',
        success: function (data) {
            if (data.success) {
                $("#message").removeClass('error');
                searchExecution();
            } else {
                $("#message").addClass('error');
                $("#startJob").prop('disabled', false);
            }
            $("#message").html(data.message);
        }
    });
});
$("#startBatchJob").on("click", function () {
    $("#message").html('');
    $("#startBatchJob").prop('disabled', true);
    var a=confirm("确定要批量并行启动batchWhere.txt中的任务吗？");
    if(a){
        $.ajax({
            url: './launchBatchJob',
            method: 'POST',
            success: function (data) {
                if (data.success) {
                    $("#message").removeClass('error');
                    searchExecution();
                } else {
                    $("#message").addClass('error');
                    $("#startJob").prop('disabled', false);
                }
                $("#message").html(data.message);
            }
        });
    }

});
searchExecution();

var searchInterval = setInterval(searchExecution, 20000);

$("#autoRefresh").change(function () {
    if (!$(this).is(':checked')) {
        clearInterval(searchInterval)
    } else {
        searchInterval = setInterval(searchExecution, 20000);
    }
});

$('#searchExecution').on("click", searchExecution);


function searchExecution() {
    $.ajax({
        url: './searchExecution',
        method: 'POST',
        success: function (data) {
            debugger;
            if (data && data.length > 0) {
                searchResult = data;
                var html = showDataView(data);
                $('#data').html(html);
                if (data[0].STATUS == 'COMPLETED' || data[0].STATUS == 'FAILED') {
                    $("#startJob").prop('disabled', false);
                }
            } else {
                $("#data").html('')
            }
        }
    })
}

function showDataView(list) {
    var html = [];
    for (var i = 0; i < list.length; i++) {
        var data = list[i];
        html.push('<tr><td>');
        html.push(data.JOB_EXECUTION_ID + '</td><td>');
        html.push(getOptHtml(data) + '</td><td>');
        html.push(toLocalDateTime(data.START_TIME) + '</td><td>');
        html.push(toLocalDateTime(data.END_TIME) + '</td><td>');
        html.push(elapsed(data.START_TIME, data.END_TIME) + '</td><td>');
        html.push(data.STATUS + '</td><td>');
        html.push(data.READ_COUNT + '</td><td>');
        html.push(data.WRITE_COUNT + '</td><td>');
        html.push(data.WRITE_SKIP_COUNT + '</td><td>');
        html.push(htmlEncode(data.where_sql) + '</td>');
        html.push('</tr>')
    }
    return html.join('\n');
}

function getOptHtml(data) {
    if (data.STATUS == 'FAILED' || data.STATUS == 'STOPPED') {
        return '<a href="javascript:void(0)">重试</a>'
    } else if (data.STATUS == 'STARTED') {
        return '<a href="javascript:void(0)">终止</a>'
    } else if (data.STATUS == 'COMPLETED') {
        return '<a href="javascript:void(0)">校验</a>'
    } else {
        return '';
    }
}

function toLocalDateTime(time) {
    if (!time) {
        return '';
    }
    return new Date(time).format('yyyy-MM-dd hh:mm:ss')
}

/**
 * 转换时间差
 * @param start
 * @param end
 * @returns {string}
 */
function elapsed(start, end) {
    debugger;
    var startDate = start.substring(0,19);
    start = new Date(startDate).getTime();
    if (!end) {
        end = new Date().getTime();
    } else {
        var endDate = end.substring(0,19);
        end = new Date(endDate).getTime();
    }
    var elapse = end - start;
    if (elapse < 1000) {
        return elapse + 'ms'
    }
    elapse = Math.round(elapse / 1000);//四舍五入
    var readable = '';
    var oneMinute = 60;
    var oneHour = 60 * 60;

    if (elapse > oneHour) {
        readable = Math.floor(elapse / oneHour) + 'h,';
        elapse = elapse % oneHour
    }
    if (elapse > oneMinute) {
        readable += Math.floor(elapse / oneMinute) + 'm,';
        elapse = elapse % oneMinute
    }
    readable += elapse + 's'
    return readable;
}

function htmlEncode(value) {
    return $('<dev/>').text(value).html();
}

$('#data').on('click', 'td', function () {
    if ($(this).text().indexOf('FAILED') > -1) {
        var index = $(this).parent().index();
        var data = searchResult[index];
        $('#dialog pre').html(data.EXIT_MESSAGE);
        $('#dialog').show();
    }
});

$('#dialog .close').click(function () {
    $('#dialog').hide();
})


$('#data').on('click', 'a', function () {
    if ($(this).text().indexOf('重试') > -1) {
        var index = $(this).parents('tr').index();
        var data = searchResult[index];
        restartJob(data.JOB_EXECUTION_ID);
    }else if ($(this).text().indexOf('终止') > -1) {
        var index = $(this).parents('tr').index();
        var data = searchResult[index];
        stopJob(data.JOB_EXECUTION_ID);
    }else if ($(this).text().indexOf('校验') > -1) {
        var index = $(this).parents('tr').index();
        var data = searchResult[index];
        var url = "./check.html?jobId="+data.JOB_EXECUTION_ID+"&recordCount="+data.WRITE_COUNT;
        window.open(url);
    }
});

function restartJob(jobId){
    $.ajax({
        url: './restartJob',
        data: {
            executionId: jobId
        },
        method: 'POST',
        success: function (data) {
            if (data.success) {
                $("#tabMsg").removeClass('error');
                searchExecution();
            } else {
                $("#tabMsg").addClass('error');
            }
            $("#tabMsg").html(data.message);
        }
    });
}

function stopJob(jobId){
    $.ajax({
        url: './stopJob',
        data: {
            executionId: jobId
        },
        method: 'POST',
        success: function (data) {
            if (data.success) {
                $("#tabMsg").removeClass('error');
                searchExecution();
            } else {
                $("#tabMsg").addClass('error');
            }
            $("#tabMsg").html(data.message);
        }
    });
}