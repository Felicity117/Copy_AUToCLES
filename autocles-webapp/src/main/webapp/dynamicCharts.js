var counter = 0;
var limit = 6;

var chartsArray=new Array();

//Fill charts array with null values
for(var i=0; i<limit; i++) {
	chartsArray[i]=null;
}

function addForm(divName,nameArray){

     if (counter == limit)  {
     
          alert("You have reached the limit of adding " + counter + " charts");
          
     } else {
     
          var newForm = document.createElement('form');
          
          var chartNameIndex = getFirstNullIndexInChartsArray();
          
          var chartName = "Chart" + chartNameIndex;
          
          //Add the chartName to the chart array
          chartsArray[chartNameIndex]=chartName;
          
          //We need to set the id of the form
          //to enable the user to remove the form for chart creation
          newForm.setAttribute('id',chartName);
         
          var newFieldSet = document.createElement('fieldset');
          
          newFieldSet.innerHTML = newForm.innerHTML.concat("<legend>"+chartName+"</legend> \n");
          
          //We add a checkbox for each kpi-name in the array
          for (i=0; i<nameArray.length; i++){

          	newFieldSet.innerHTML = newFieldSet.innerHTML.concat("<input type='checkbox' id='"+chartName+"|"+nameArray[i]+"' /> Show kpi "+nameArray[i]+" <br/>\n");
          
          }
          
          //Add the textbox to insert the sampling time
          newFieldSet.innerHTML = newFieldSet.innerHTML.concat("<br/>samplingTime:	<input type='text' id='"+chartName+"|samplingTime' value = '5000' onkeypress='return onlyNumbers();'\><br/>");
          
          //Add the textbox to insert the number of sample
          newFieldSet.innerHTML = newFieldSet.innerHTML.concat("sampleSize:		<input type='text' id='"+chartName+"|sampleSize' value = '30'  onkeypress='return onlyNumbers();'\><br/>");
          
          
          //Add a cehckbox to decide if we want to append data
          newFieldSet.innerHTML = newFieldSet.innerHTML.concat("<input type='checkbox' id='"+chartName+"|appendData"+"' checked/> AppendData <br/>\n");
          
          
          //Add a cehckbox to decide if we want to shift data
          newFieldSet.innerHTML = newFieldSet.innerHTML.concat("<input type='checkbox' id='"+chartName+"|shiftData"+"' checked/> shiftData <br/>\n");
          
          
          //Also add the button to remove the form for the chart creation
          newFieldSet.innerHTML = newFieldSet.innerHTML.concat("<input type='button' value='Remove chart'"+" onClick=\"removeForm('"+divName+"','"+chartNameIndex+"');\"/> <br/>\n");
          
          newForm.appendChild(newFieldSet);
          
          //Here is where the new form is added to the div divName
          document.getElementById(divName).appendChild(newForm);
          counter++;
          
     }
}

function removeForm(divName, chartNameIndex) {
  	var divElement = document.getElementById(divName);
  	var formToRemove = document.getElementById("Chart"+chartNameIndex);
  	divElement.removeChild(formToRemove);
  	counter--;
  	
  	//remove from charts array
  	chartsArray[chartNameIndex]=null;
}

function onlyNumbers(evt)
{
    var e = event || evt; // for trans-browser compatibility
    var charCode = e.which || e.keyCode;

    if (charCode > 31 && (charCode < 48 || charCode > 57))
        return false;

    return true;

}

function getFirstNullIndexInChartsArray(){

	  for(var i=0; i<limit; i++) {
          		
          		//fill the first null value
				if(!chartsArray[i]){
					return i;
				}else{
					//value not null
				}
		}
	
	//no non-null value has been found		
	return -1;
}

function generateCharts(kpisNamesArray){

 	var chartsURL="/mepgui/charts?";
 	
 	var allChartsParametersArray;
 	
 	for(var i=0; i<limit; i++) {
 	
 		//if the field is null does nothing
 		//it means that the chart must not be generated
		if(!chartsArray[i]){
			
		}else{
		
			chartsURL+=chartsArray[i]+"=";
			
			//read the values of the parameters for
			//this chart
			var samplingTime=document.getElementById([chartsArray[i]+'|samplingTime']).value;
			
			//alert("sampligTime for "+chartsArray[i]+": "+samplingTime);
			
			chartsURL+=samplingTime+",";
			
			var sampleSize=document.getElementById([chartsArray[i]+'|sampleSize']).value;
			
			//alert("sampleSize for "+chartsArray[i]+": "+sampleSize);
			
			chartsURL+=sampleSize+",";
			
			if(document.getElementById([chartsArray[i]+'|appendData']).checked){
				chartsURL+="true,"
			}else{
				chartsURL+="false,"
			}
			
			if(document.getElementById([chartsArray[i]+'|shiftData']).checked){
				chartsURL+="true,"
			}else{
				chartsURL+="false,"
			}
			
			
		//	chartsURL+=sampleSize+",true,true,";
			
			//var kpisToDisplay = new Array();
			
			//loop on the kpis names
			//to see which kpis
			//we must display in the graph
			for(var j=0; j<kpisNamesArray.length; j++) {
			
				if(document.getElementById([chartsArray[i]+"|"+kpisNamesArray[j]]).checked){
					//we have to display this kpi
					//kpisToDisplay.push(kpisNamesArray[j]);
					chartsURL+=kpisNamesArray[j]+":";
					
					//alert("KPI found "+chartsArray[i]+": "+kpisNamesArray[j]);
				}
			}
			
			//remove trailing ":"
			chartsURL = chartsURL.substring(0, chartsURL.length-1);
			
			//At this point we have all the information
			//we need to know how to display this specific charts
			//we save all the information in an array
			//and then we save this array in the global
			//array containing all the charts parameters arrays
// 			var chartParametersArray;
// 			chartParametersArray.push(samplingTime);
// 			chartParametersArray.push(sampleSize);
// 			chartParametersArray.push(kpisToDisplay);
// 			
// 			allChartsParametersArray.push(chartParametersArray);

			chartsURL+="&";
			
			
			//alert("chartsURL: "+chartsURL);
		}
 	}
 	
 	//remove trailing "&"
	chartsURL = chartsURL.substring(0, chartsURL.length-1);
 	
 	//At this point we have all the parameters
 	//of all the charts we want to display
 	//we can now use these parameters to generate the
 	//charts URL
 	 	 
 	//Here the redirection occurs
	window.location = chartsURL;
}
