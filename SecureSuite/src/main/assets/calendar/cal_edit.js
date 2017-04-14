app.controller('calEditController', ['$scope','$location','$http','calService',
                          function(   $scope,  $location,  $http,  calService) {

  $scope.year = new Date().getFullYear(); // footer copyright year

  $scope.NewEvent = calService.get();
  $scope.event_title = $scope.NewEvent.title;
  $scope.event_description = $scope.NewEvent.description;
  $scope.start_time = $scope.NewEvent.start;
  $scope.end_time = $scope.NewEvent.end;
  $scope.notifications = $scope.NewEvent.notifications;

  $scope.alertMessage = "";
  $scope.unitNames = ["minutes","hours","days","weeks","years"];

      $scope.addNotification = function(){

          var defaultNotification = {count: 30, units: 'minutes'}
          $scope.notifications.push( defaultNotification );

//          angular.forEach($scope.NewEvent.notifications, function (value) {
//
//              console.log("notifications: "+value.count+", "+value.units);
//          });
      }
      $scope.save = function(){
          $scope.NewEvent.start = $scope.sharedStartDate;
          $scope.NewEvent.end = $scope.sharedEndDate;

          $http.post("/calendar/save", {data: $scope.NewEvent} )
          .then(function (response) {

              if (response.data.success) {
                  $location.path('cal');
              }else{
                  $scope.alertMessage = "Save error";
              }
          })
      }

      $scope.cancel = function(){

          $location.path('cal');
      }
      $scope.sharedStartDate = $scope.NewEvent.start; // (formatted: 3/13/17 3:30 PM)
      $scope.sharedEndDate = $scope.NewEvent.end; // (formatted: 3/13/17 3:30 PM)

      $('#colorselector').colorselector();
      $('#colorselector').colorselector('setColor', '#87CEFA');

      $('#colorselector').colorselector({
            callback: function (value, color, title) {
console.log("color: "+color);
            }
      });

}]);

