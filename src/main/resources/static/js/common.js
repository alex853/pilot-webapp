
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
