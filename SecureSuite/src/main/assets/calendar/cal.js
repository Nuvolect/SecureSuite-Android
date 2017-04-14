
app.controller('calController', function($scope, $http, uiCalendarConfig, $uibModal) {

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
    // this function clears clender enents
    function clearCalendar() {
        if (uiCalendarConfig.calendars.myCalendar != null) {
            uiCalendarConfig.calendars.myCalendar.fullCalendar('removeEvents');
            uiCalendarConfig.calendars.myCalendar.fullCalendar('unselect');
        }
    }
    //Load events from server to display on calendar
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
//debugger;// is value.notifications defined?
                $scope.events.push({
                    id: value.id,
                    title: value.title,
                    description: value.description,
                    start: value.start,
                    end: value.end,
                    allDay: value.allDay,
                    notifications: value.notifications,
                    stick: true
                });
            });
        });
    }
    <!--populate();-->

    //UI- calendar configuration
    $scope.uiConfig = {
        calendar: {
            utc: true,
            transmitTZD: true,
            allDay: false,
            height: 450,
            editable: true,
            displayEventTime: false,
            header: {
                left:'today prev,next title',
                center: '',
                right: 'month,agendaWeek,agendaDay,listDay,listMonth,listWeek,listYear'
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
                $scope.NewEvent = {
                    id: 0,
                    start: start,
                    end: end,
                    allDay: false,
                    title: '',
                    description: '',
                    notifications:[
                        {count: 30, units: 'minutes'}
                    ]
                }
                $scope.showModal();
            },
            eventClick: function (event) {
                $scope.SelectedEvent = event;
//debugger;
                $scope.NewEvent = {
                    id: event.id,
                    start: event.start,
                    end: event.end,
                    allDay: false,
                    title: event.title,
                    description: event.description,
                    notifications : event.notifications
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

    //This function shows bootstrap modal dialog
    $scope.showModal = function () {
//debugger;
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
            $scope.NewEvent.start = data.event.start.format();
            $scope.NewEvent.end = data.event.end.format();

            switch (data.operation) {

                case 'save':            //save
//debugger;
                    $http.post("/calendar/save", {data: $scope.NewEvent} )
                    .then(function (response) {

                        if (response.data.success) {
                            populate();
                        }
                    })
                    break;

                case 'delete':          //delete

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

})

app.controller('modalController',
            ['$scope','$location','calService','$uibModalInstance','NewEvent',
    function ($scope,  $location,  calService,  $uibModalInstance,  NewEvent) {

    $scope.NewEvent = NewEvent;
    $scope.Message = "";

    var fromDate = moment(NewEvent.start).format('YYYY/MM/DD LT');
    var endDate = moment(NewEvent.end).format('YYYY/MM/DD LT');
    $scope.SlotRange = fromDate + " - " + endDate;

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

