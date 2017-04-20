// Copyright (c) 2017. Nuvolect LLC
//
// This program is free software: you can redistribute it and/or modify it under the terms of the GNU
// General Public License as published by the Free Software Foundation, either version 3 of the License,
// or (at your option) any later version.
//
// Contact legal@nuvolect.com for a less restrictive commercial license if you would like to use the
// software without the GPLv3 restrictions.
//
// This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
// even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along with this program.  If not,
//  see <http://www.gnu.org/licenses/>.


app.controller('calController',
    function ($scope, $location, $http, calService, uiCalendarConfig, $uibModal) {

  $scope.year = new Date().getFullYear(); // footer copyright year

   $scope.SelectedEvent = null;
    var isFirstTime = true;
    $scope.events = [];
    $scope.eventSources = [$scope.events];

    $scope.NewEvent = {};
    //this function for get datetime from json date
    function getDate(datetime) {
        if (datetime != null) {
            var mili = datetime.replace(/\/Date\((-?\d+)\)\//, '$1');
            return new Date(parseInt(mili));
        }
        else {
            return "";
        }
    }
    // Clears calendar events
    function clearCalendar() {
        if (uiCalendarConfig.calendars.myCalendar != null) {
            uiCalendarConfig.calendars.myCalendar.fullCalendar('removeEvents');
            uiCalendarConfig.calendars.myCalendar.fullCalendar('unselect');
        }
    }
    // Load events from server to display on calendar
    function populate() {
        clearCalendar();

        var view = uiCalendarConfig.calendars.myCalendar.fullCalendar('getView');
        var start = view.start.format();
        var end = view.end.format();
        $http.get('/calendar/getevents', {
            cache: false,
            params: { "start": start, "end" : end}
        }).then(function (data) {
            $scope.events.slice(0, $scope.events.length);

            angular.forEach(data.data, function (value) {

                $scope.events.push({
                    id: value.id,
                    title: value.title,
                    description: value.description,
                    start: value.start,
                    end: value.end,
                    allDay: value.allDay,
                    notifications: value.notifications,
                    color: value.color,
                    stick: true
                });
            });
        });
    }
    <!--populate();-->

    //UI- calendar configuration
    $scope.uiConfig = {
        calendar: {
            timezone: 'local',
            allDay: false,
            height: 450,
            editable: true,
            displayEventTime: false,
            header: {
                left:'today prev,next title',
                center: '',
                right: 'month,agendaWeek,agendaDay,listWeek'
            },
            timeFormat: {
                month: ' ', // for hide on month view
                agenda: 'h:mm t'
            },
            selectable: true,
            selectHelper: true,
            events: '/calendar/events',

            refetchResourcesOnNavigate: true,
            resources: '/calendar/resources',

            select: function(start, end){

                $scope.createNewEvent( 0, start, end);
                $scope.showModal();
            },
            eventClick: function (event) {
                $scope.SelectedEvent = event;

                    $scope.NewEvent = {
                        id: event.id,
                        start: event.start,
                        end: event.end,
                        allDay: false,
                        title: event.title,
                        description: event.description,
                        notifications : event.notifications,
                        color : event.color
                    }
                $scope.showModal();
            },
            eventAfterAllRender: function () {
                if ($scope.events.length > 0 && isFirstTime) {
                    uiCalendarConfig.calendars.myCalendar.fullCalendar('gotoDate', $scope.events[0].start);
                    isFirstTime = false;
                }
            }
        }
    };

    $scope.createNewEvent = function ( id, start, end) {

        $scope.NewEvent = {
            id: id,
            start: start,
            end: end,
            allDay: false,
            title: '',
            description: '',
            color: '#67C2E0',
            notifications:[
                    {count: 30, units: 'minutes'}
                ]
        }
    };

    //This function shows bootstrap modal dialog
    $scope.showModal = function () {

        $scope.option = {
            templateUrl: 'cal_modal.htm',
            controller: 'modalController',
            backdrop: 'static',
            resolve: {
                NewEvent: function () {
                    return $scope.NewEvent;
                }
            }
        };
        //CRUD operations on Calendar starts here
        var modal = $uibModal.open($scope.option);

        modal.result.then(function (data) {

            $scope.NewEvent = data.event;

            switch (data.operation) {

                case 'save':

                    $http.post("/calendar/save", {data: $scope.NewEvent} )
                    .then(function (response) {

                        if (response.data.success) {
                            populate();
                        }
                    })
                    break;

                case 'delete':

                    $http.post("/calendar/delete", {data: $scope.NewEvent} )
                    .then(function (response) {

                        if (response.data.success) {
                            populate();
                        }
                    })
                    break;
                default:
                    break;
            }
        }, function () {
//            console.log('Modal dialog closed');
        })
    }
    $scope.createEvent = function () {

        $scope.createNewEvent( 0, '', '');
        // Save to be picked up in destination page controller
        calService.set($scope.NewEvent);

        $location.path('cal_edit');
    }

});

app.controller('modalController',
            ['$scope','$location','calService','$uibModalInstance','NewEvent',
    function ($scope,  $location,  calService,  $uibModalInstance,  NewEvent) {

    $scope.NewEvent = NewEvent;
    $scope.Message = "";

    var fromDate = moment(NewEvent.start).format('MM/DD/YYYY LT');
    var endDate = moment(NewEvent.end).format('MM/DD/YYYY LT');
    $scope.slot_range = fromDate + " - " + endDate;

    $scope.save = function () {
        if ($scope.NewEvent.title.trim() != "") {
            $uibModalInstance.close({ event: $scope.NewEvent, operation: 'save'});
        }
        else {
            $scope.Message = "Event title required!";
        }
    }
    $scope.edit = function () {

        // Save to be picked up in destination page controller
        calService.set($scope.NewEvent);

        $uibModalInstance.dismiss('cancel');

        $location.path('cal_edit');
    }
    $scope.delete = function () {
        $uibModalInstance.close({ event: $scope.NewEvent, operation: 'delete' });
    }
    $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
    }
}]);

