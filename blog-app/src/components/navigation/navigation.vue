<template>
 <div ref="links" v-title data-title="导航" class="sectionImg">
	<div class="nav-card">
		<div class="nav-card-top">
<!--			<div class="image" :style="{'background-image': 'url('+'http://cos.myo.pub/icon/'+imgicon+')'}">-->
      <div class="image" style='background: url("https://www.loliapi.com/acg/")'>
			</div>
		</div>
	  <div class="content">
		<a href="#">
		  <span class="title">
			{{name}}
		  </span>
		</a>

		<p class="desc">
		  {{content}}
		</p>

		<a class="action" :href="url">
		  Find out more
		  <span aria-hidden="true">
			→
		  </span>
		</a>
	  </div>
	</div>
</div>
</template>

<script>
    export default {
        name: "navigation",
        data(){
            return {

			}
        },
		props: {
		  id: String,
		  content: String,
		  imgicon: String,
		  name: String,
		  url: String,
		  index:Number,
		},
    }
</script>

<style scoped>
	.nav-card {
	  max-width: 300px;
	  border-radius: 0.5rem;
	  background-color: #fff;
	  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
	  border: 1px solid transparent;
	  margin-bottom: 20px;
	}

	.nav-card a {
	  text-decoration: none
	}

	.content {
	  padding: 1.1rem;
	}
	.nav-card-top{
		object-fit: cover;
		width: 100%;
		height: 150px;
	}
	.image {
	  height: 100%;
	  background-position:  center;
	  background-repeat: no-repeat;
	  background-size: cover;
	  background-color: rgb(239, 205, 255);

	}

	.title {
	  color: #111827;
	  font-size: 1.125rem;
	  line-height: 1.75rem;
	  font-weight: 600;
	}

	.desc {
	  margin-top: 0.5rem;
	  color: #6B7280;
	  font-size: 0.875rem;
	  line-height: 1.25rem;
	}

	.action {
	  display: inline-flex;
	  margin-top: 1rem;
	  color: #ffffff;
	  font-size: 0.875rem;
	  line-height: 1.25rem;
	  font-weight: 500;
	  align-items: center;
	  gap: 0.25rem;
	  background-color: #2563EB;
	  padding: 4px 8px;
	  border-radius: 4px;
	}

	.action span {
	  transition: .3s ease;
	}

	.action:hover span {
	  transform: translateX(4px);
	}

</style>
