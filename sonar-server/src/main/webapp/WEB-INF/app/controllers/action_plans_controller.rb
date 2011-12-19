#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

class ActionPlansController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE
  before_filter :load_resource
  verify :method => :post, :only => [:save, :delete, :change_status], :redirect_to => {:action => :index}

  def index
    @action_plans = ActionPlan.find(:all, :conditions => ['project_id=?', @resource.id], :include => 'reviews', :order => 'dead_line ASC')
  end

  def new
    if params[:name] || params[:description] || params[:dead_line]
      @action_plan = ActionPlan.new
    elsif params[:plan_id]
      @action_plan = ActionPlan.find params[:plan_id]
    end
  end

  def save
    @action_plan = ActionPlan.find params[:plan_id] unless params[:plan_id].blank?
    unless @action_plan
      @action_plan = ActionPlan.new(:user_login => current_user.login,
                                    :project_id => @resource.id,
                                    :status => ActionPlan::STATUS_OPEN)
    end
    @action_plan.name = params[:name]
    @action_plan.description = params[:description]
    begin
      @action_plan.dead_line = Date.strptime(params[:dead_line], '%d/%m/%Y') unless params[:dead_line].blank?
    rescue
      date_not_valid = message('action_plans.date_not_valid')
    end

    if date_not_valid || !@action_plan.valid?
      @action_plan.errors.add :dead_line, date_not_valid if date_not_valid
      render :action => :new, :id => @resource.id
    else
      @action_plan.save
      redirect_to :action => 'index', :id => @resource.id
    end
  end

  def delete
    action_plan = ActionPlan.find params[:plan_id]
    action_plan.destroy
    redirect_to :action => 'index', :id => @resource.id
  end

  def change_status
    action_plan = ActionPlan.find params[:plan_id]
    if action_plan
      action_plan.status = action_plan.open? ? ActionPlan::STATUS_CLOSED : ActionPlan::STATUS_OPEN
      action_plan.save
    end
    redirect_to :action => 'index', :id => @resource.id
  end

  private

  def load_resource
    @resource=Project.by_key(params[:id])
    return redirect_to home_path unless @resource
    access_denied unless has_role?(:admin, @resource)
  end

end