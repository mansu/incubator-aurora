'use strict';

/* Controllers */

angular.module('auroraUI.controllers', []).
    controller('JobSummaryController',
    function ($scope, $window, auroraClient) {
      $scope.title = 'Scheduled Jobs Summary';

      $scope.error = false;
      $scope.errorMsg = "An error occurred when querying the server. Please reload this page.";

      $scope.columnCollection = [
        {label : 'Role', map: 'role', cellTemplateUrl: 'roleLink.html'},
        {label : 'Jobs', map: 'jobCount'},
        {label : 'Cron Jobs', map: 'cronJobCount'}
      ];

      $scope.rowCollection = parseResponse(auroraClient.getJobSummary());

      function parseResponse(response) {
        $scope.error = response.error;
        return response.summaries;
      }

      $scope.globalConfig = {
        isGlobalSearchActivated: true,
        isPaginationEnabled: true,
        itemsByPage: 25,
        maxSize: 8
      };
   });
