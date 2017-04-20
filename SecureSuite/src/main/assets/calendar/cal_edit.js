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
// see <http://www.gnu.org/licenses/>.

app.controller('calEditController', ['$scope','$location','$http','calService',
                          function(   $scope,  $location,  $http,  calService) {

  $scope.year = new Date().getFullYear(); // footer copyright year

  $scope.NewEvent = calService.get();
  $scope.event_title = $scope.NewEvent.title;
  $scope.event_description = $scope.NewEvent.description;
  $scope.start_time = $scope.NewEvent.start;
  $scope.end_time = $scope.NewEvent.end;
  $scope.notifications = $scope.NewEvent.notifications;

  $scope.sharedStartDate = $scope.NewEvent.start; // (formatted: 3/13/17 3:30 PM)
  $scope.sharedEndDate = $scope.NewEvent.end; // (formatted: 3/13/17 3:30 PM)

  $scope.colorData = { "color": $scope.NewEvent.color };

  $scope.repeatName = "Weekly";
  $scope.repeatCB = false;

  $scope.alertMessage = "";
  $scope.unitNames = ["minutes","hours","days","weeks","years"];
  $scope.repeatNames = [
      "Daily",
      "Every weekday (Monday to Friday)",
      "Every Monday, Wednesday, and Friday",
      "Weekly",
      "Monthly",
      "Yearly"
      ];

  $scope.addNotification = function(){

      var defaultNotification = {count: 30, units: 'minutes'}
      $scope.notifications.push( defaultNotification );

//          angular.forEach($scope.NewEvent.notifications, function (value) {
//
//              console.log("notifications: "+value.count+", "+value.units);
//          });
  }
  $scope.save = function(){

      $scope.NewEvent.title = $scope.event_title;
      $scope.NewEvent.description = $scope.event_description;
      $scope.NewEvent.start = $scope.sharedStartDate;
      $scope.NewEvent.end = $scope.sharedEndDate;
      $scope.NewEvent.color = $scope.colorData.color;
      $scope.NewEvent.notifications = $scope.notifications;

      $http.post("/calendar/save", {data: $scope.NewEvent} )
      .then(function (response) {

          if (response.data.success) {
              $location.path('cal');
          }else{
              $scope.alertMessage = "Save error";
          }
      })
  }

  // Back to main page when user cancels
  $scope.cancel = function(){

      $location.path('cal');
  }

  $scope.remove = function(array, index){
    array.splice(index, 1);
}
}]);

