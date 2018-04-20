import React from 'react';
import PropTypes from 'prop-types';
import StatusIndicator from '../EntryViews/StatusIndicator.jsx';

class StorageSelector extends React.Component {
    static propTypes = {
        selectedStorage: PropTypes.number.isRequired,
        selectionUpdated: PropTypes.func.isRequired,
        storageList: PropTypes.array.isRequired,
        enabled: PropTypes.bool.isRequired,
        showLabel: PropTypes.bool
    };

    getSelectedStorageRecord(){
        const results = this.props.storageList.filter(entry=>entry.id===this.props.selectedStorage);
        if(results.length>0){
            return results[0];
        }  else return null;
    }

    getSelectedStatus(){
        console.log(this.getSelectedStorageRecord());

        if(!this.getSelectedStorageRecord()) return "hidden";
        return this.getSelectedStorageRecord().status
    }

    render(){
        return <span>
            <select id="storageSelector" value={this.props.selectedStorage}
                    disabled={! this.props.enabled} style={{marginRight: "1em"}}
                    onChange={event=>this.props.selectionUpdated(parseInt(event.target.value))}>
            {
                this.props.storageList.map(storage=><option key={storage.id} value={storage.id}>{storage.rootpath} on {storage.storageType}</option>)
            }
        </select>
        <StatusIndicator status={this.getSelectedStatus()} showLabel={this.props.showLabel}/>
        </span>
    }
}

export default StorageSelector;