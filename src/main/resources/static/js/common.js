
function modifyIcao(propertyName) {
    var currentIcao = $('#' + propertyName).text();

    var icao = prompt('Specify ICAO please', currentIcao);
    if (!icao) {
        return;
    }

    icao = icao.toUpperCase();

    $.ajax({
        url: '/set',
        method: 'POST',
        data: {
            property: propertyName,
            value: icao
        },
        success: function () {
            $('#' + propertyName).text(icao);
        },
        error: function () {
            alert('Error happened!');
        }
    });
}

function modifyTime(propertyName) {
    var currentTime = $('#' + propertyName).text();

    if (currentTime === undefined || currentTime === '') {
        alert("Can not edit time of an event that still not happened");
        return;
    }

    var icao = prompt('Specify time', currentTime);
    if (!icao) {
        return;
    }

    icao = icao.toUpperCase();

    $.ajax({
        url: '/set',
        method: 'POST',
        data: {
            property: propertyName,
            value: icao
        },
        success: function () {
            $('#' + propertyName).text(icao);
        },
        error: function () {
            alert('Error happened!');
        }
    });
}
